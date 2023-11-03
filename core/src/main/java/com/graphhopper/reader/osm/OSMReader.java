/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.reader.osm;

import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.cursors.LongCursor;
import com.graphhopper.coll.GHLongLongHashMap;
import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.ReaderNode;
import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.dem.EdgeElevationSmoothingMovingAverage;
import com.graphhopper.reader.dem.EdgeElevationSmoothingRamer;
import com.graphhopper.reader.dem.EdgeSampling;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.OSMReaderConfig;
import com.graphhopper.routing.ev.Country;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.State;
import com.graphhopper.routing.util.AreaIndex;
import com.graphhopper.routing.util.CustomArea;
import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.routing.util.OSMParsers;
import com.graphhopper.routing.util.countryrules.CountryRule;
import com.graphhopper.routing.util.countryrules.CountryRuleFactory;
import com.graphhopper.routing.util.parsers.RestrictionSetter;
import com.graphhopper.search.KVStorage;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.TurnCostStorage;
import com.graphhopper.util.*;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.LongToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.graphhopper.search.KVStorage.KeyValue.*;
import static com.graphhopper.util.GHUtility.OSM_WARNING_LOGGER;
import static com.graphhopper.util.Helper.nf;
import static java.util.Collections.emptyList;

/**
 * Parses an OSM file (xml, zipped xml or pbf) and creates a graph from it. The OSM file is actually read twice.
 * During the first scan we determine the 'type' of each node, i.e. we check whether a node only appears in a single way
 * or represents an intersection of multiple ways, or connects two ways. We also scan the relations and store them for
 * each way ID in memory.
 * During the second scan we store the coordinates of the nodes that belong to ways in memory and then split each way
 * into several segments that are divided by intersections or barrier nodes. Each segment is added as an edge of the
 * resulting graph. Afterwards we scan the relations again to determine turn restrictions.
 **/
/**
 * Chúng ta đang phân tích một tệp OSM (có thể là xml, xml đã nén hoặc pbf) để tạo ra một đồ thị. 
 * Quá trình này sẽ đọc tệp OSM hai lần.
 * 
 * Trong lần đọc đầu tiên, 
 * chúng ta xác định 'loại' của mỗi nút. 
 * Cụ thể, chúng ta kiểm tra xem một nút có chỉ xuất hiện trong một đường duy nhất hay không, 
 * hay nó có là điểm giao nhau của nhiều đường, 
 * hoặc nó có kết nối hai đường với nhau hay không. 
 * Chúng ta cũng quét qua các mối quan hệ và lưu chúng lại dựa trên ID của mỗi đường.
 * 
 * Trong lần đọc thứ hai, 
 * chúng ta lưu lại tọa độ của các nút thuộc về các đường và sau đó chia mỗi đường thành nhiều phân đoạn. 
 * Các phân đoạn này được chia bởi các điểm giao nhau hoặc các nút chướng ngại vật. 
 * Mỗi phân đoạn sẽ được thêm vào như một cạnh của đồ thị kết quả. 
 * Sau cùng, chúng ta quét lại các mối quan hệ để xác định các hạn chế về lượt.
 **/

public class OSMReader {
    // Khởi tạo logger
    private static final Logger LOGGER = LoggerFactory.getLogger(OSMReader.class);

    // Khởi tạo một pattern để tách tên của các đường
    private static final Pattern WAY_NAME_PATTERN = Pattern.compile("; *");

    // Các biến cần thiết cho việc đọc file OSM và xử lý dữ liệu
    private final OSMReaderConfig config;
    private final BaseGraph baseGraph;
    private final EdgeIntAccess edgeIntAccess;
    private final NodeAccess nodeAccess;
    private final TurnCostStorage turnCostStorage;
    private final OSMParsers osmParsers;
    private final DistanceCalc distCalc = DistanceCalcEarth.DIST_EARTH;
    private final RestrictionSetter restrictionSetter;
    private ElevationProvider eleProvider = ElevationProvider.NOOP;
    private AreaIndex<CustomArea> areaIndex;
    private CountryRuleFactory countryRuleFactory = null;
    private File osmFile;
    private final RamerDouglasPeucker simplifyAlgo = new RamerDouglasPeucker();

    // Các biến tạm thời và bộ nhớ đệm
    private final IntsRef tempRelFlags;
    private Date osmDataDate;
    private long zeroCounter = 0;

    // Các biến lưu trữ thông tin về các quan hệ và cạnh trong đồ thị
    private GHLongLongHashMap osmWayIdToRelationFlagsMap = new GHLongLongHashMap(200, .5f);
    private WayToEdgesMap restrictedWaysToEdgesMap = new WayToEdgesMap();
    private List<ReaderRelation> restrictionRelations = new ArrayList<>();

    // Hàm khởi tạo cho lớp OSMReader
    public OSMReader(BaseGraph baseGraph, OSMParsers osmParsers, OSMReaderConfig config) {
        this.baseGraph = baseGraph;
        this.edgeIntAccess = baseGraph.createEdgeIntAccess();
        this.config = config;
        this.nodeAccess = baseGraph.getNodeAccess();
        this.osmParsers = osmParsers;
        this.restrictionSetter = new RestrictionSetter(baseGraph);

        simplifyAlgo.setMaxDistance(config.getMaxWayPointDistance());
        simplifyAlgo.setElevationMaxDistance(config.getElevationMaxWayPointDistance());
        turnCostStorage = baseGraph.getTurnCostStorage();

        tempRelFlags = osmParsers.createRelationFlags();
        if (tempRelFlags.length != 2)
            // Chúng ta hiện đang sử dụng một giá trị long để lưu trữ các cờ quan hệ, vì vậy các cờ quan hệ ints ref phải có độ dài là 2
            throw new IllegalArgumentException("OSMReader không thể sử dụng các cờ quan hệ với số nguyên khác 2");
    }


    /**
     * Đặt tệp OSM để đọc. Các định dạng được hỗ trợ bao gồm .osm.xml, .osm.gz và .xml.pbf
     */
    public OSMReader setFile(File osmFile) {
        this.osmFile = osmFile;
        return this;
    }

    /**
     * Chỉ mục khu vực được truy vấn cho mỗi cách OSM và các khu vực liên quan được thêm vào các thẻ của cách
     */
    public OSMReader setAreaIndex(AreaIndex<CustomArea> areaIndex) {
        this.areaIndex = areaIndex;
        return this;
    }

    public OSMReader setElevationProvider(ElevationProvider eleProvider) {
        if (eleProvider == null)
            throw new IllegalStateException("Sử dụng nhà cung cấp độ cao NOOP thay vì null hoặc không gọi setElevationProvider");

        if (!nodeAccess.is3D() && ElevationProvider.NOOP != eleProvider)
            throw new IllegalStateException("Đảm bảo rằng đồ thị của bạn chấp nhận dữ liệu 3D");

        this.eleProvider = eleProvider;
        return this;
    }

    public OSMReader setCountryRuleFactory(CountryRuleFactory countryRuleFactory) {
        this.countryRuleFactory = countryRuleFactory;
        return this;
    }

    public void readGraph() throws IOException {
        if (osmParsers == null)
            throw new IllegalStateException("Các trình phân tích thẻ không được đặt.");

        if (osmFile == null)
            throw new IllegalStateException("Không có tệp OSM nào được chỉ định");

        if (!osmFile.exists())
            throw new IllegalStateException("Tệp OSM bạn chỉ định không tồn tại:" + osmFile.getAbsolutePath());

        if (!baseGraph.isInitialized())
            throw new IllegalStateException("BaseGraph phải được khởi tạo trước khi chúng ta có thể đọc OSM");

        WaySegmentParser waySegmentParser = new WaySegmentParser.Builder(baseGraph.getNodeAccess(), baseGraph.getDirectory())
                .setElevationProvider(eleProvider)
                .setWayFilter(this::acceptWay)
                .setSplitNodeFilter(this::isBarrierNode)
                .setWayPreprocessor(this::preprocessWay)
                .setRelationPreprocessor(this::preprocessRelations)
                .setRelationProcessor(this::processRelation)
                .setEdgeHandler(this::addEdge)
                .setWorkerThreads(config.getWorkerThreads())
                .build();
        waySegmentParser.readOSM(osmFile);
        osmDataDate = waySegmentParser.getTimeStamp();
        if (baseGraph.getNodes() == 0)
            throw new RuntimeException("Đồ thị sau khi đọc OSM không được để trống");
        releaseEverythingExceptRestrictionData();
        addRestrictionsToGraph();
        releaseRestrictionData();
        LOGGER.info("Đã hoàn thành việc đọc tệp OSM: {}, nodes: {}, edges: {}, zero distance edges: {}",
                osmFile.getAbsolutePath(), nf(baseGraph.getNodes()), nf(baseGraph.getEdges()), nf(zeroCounter));
    }


    /**
     * @return thời gian được ghi trong tiêu đề tệp OSM hoặc null nếu không tìm thấy
     */
    public Date getDataDate() {
        return osmDataDate;
    }

    /**
     * Phương thức này được gọi cho mỗi cách trong lần đầu tiên và lần thứ hai của {@link WaySegmentParser}. Tất cả OSM
     * các cách không được chấp nhận ở đây và tất cả các nút không được tham chiếu bởi bất kỳ cách nào như vậy sẽ bị bỏ qua.
     */
    protected boolean acceptWay(ReaderWay way) {
        // bỏ qua hình học bị hỏng
        if (way.getNodes().size() < 2)
            return false;

        // bỏ qua hình học đa giác
        if (!way.hasTags())
            return false;

        return osmParsers.acceptWay(way);
    }

    /**
     * @return true nếu nút đã cho nên được nhân đôi để tạo một cạnh nhân tạo. Nếu nút trở thành một
     * giao điểm giữa các cách khác nhau, điều này sẽ bị bỏ qua và không có cạnh nhân tạo nào sẽ được tạo.
     */
    protected boolean isBarrierNode(ReaderNode node) {
        return node.hasTag("barrier") || node.hasTag("ford");
    }

    /**
     * @return true nếu chiều dài của cách phải được tính toán và thêm vào như một thẻ cách nhân tạo
     */
    protected boolean isCalculateWayDistance(ReaderWay way) {
        return isFerry(way);
    }

    private boolean isFerry(ReaderWay way) {
        return FerrySpeedCalculator.isFerry(way);
    }

    /**
     * Phương thức này được gọi trong lần thứ hai của {@link WaySegmentParser} và cung cấp một điểm vào để làm giàu
     * các thẻ OSM đã cho với các thẻ bổ sung trước khi nó được chuyển tiếp đến các trình phân tích thẻ.
     */
    protected void setArtificialWayTags(PointList pointList, ReaderWay way, double distance, List<Map<String, Object>> nodeTags) {
        way.setTag("node_tags", nodeTags);
        way.setTag("edge_distance", distance);
        way.setTag("point_list", pointList);

        // chúng ta phải xóa các thẻ nhân tạo hiện có, vì chúng ta sửa đổi cách mặc dù có thể có nhiều cạnh
        // theo cách. sớm hay muộn chúng ta nên tách các thẻ nhân tạo ('edge') khỏi cách, xem cuộc thảo luận ở đây:
        // https://github.com/graphhopper/graphhopper/pull/2457#discussion_r751155404
        way.removeTag("country");
        way.removeTag("country_rule");
        way.removeTag("custom_areas");

        List<CustomArea> customAreas;
        if (areaIndex != null) {
            double middleLat;
            double middleLon;
            if (pointList.size() > 2) {
                middleLat = pointList.getLat(pointList.size() / 2);
                middleLon = pointList.getLon(pointList.size() / 2);
            } else {
                double firstLat = pointList.getLat(0), firstLon = pointList.getLon(0);
                double lastLat = pointList.getLat(pointList.size() - 1), lastLon = pointList.getLon(pointList.size() - 1);
                middleLat = (firstLat + lastLat) / 2;
                middleLon = (firstLon + lastLon) / 2;
            }
            customAreas = areaIndex.query(middleLat, middleLon);
        } else {
            customAreas = emptyList();
        }


        // xử lý đặc biệt cho các quốc gia: vì chúng được tích hợp sẵn với GraphHopper nên chúng luôn được cung cấp cho EncodingManager
        Country country = Country.MISSING;
        State state = State.MISSING;
        double countryArea = Double.POSITIVE_INFINITY;
        for (CustomArea customArea : customAreas) {
            // bỏ qua các khu vực không phải là quốc gia
            if (customArea.getProperties() == null) continue;
            String alpha2WithSubdivision = (String) customArea.getProperties().get(State.ISO_3166_2);
            if (alpha2WithSubdivision == null)
                continue;

            // chuỗi quốc gia phải là một cái gì đó giống như US-CA (bao gồm phân khu) hoặc chỉ DE
            String[] strs = alpha2WithSubdivision.split("-");
            if (strs.length == 0 || strs.length > 2)
                throw new IllegalStateException("Alpha2 không hợp lệ: " + alpha2WithSubdivision);
            Country c = Country.find(strs[0]);
            if (c == null)
                throw new IllegalStateException("Quốc gia không xác định: " + strs[0]);

            if (
                // các quốc gia có phân khu vượt trội hơn những quốc gia không có phân khu cũng như những quốc gia lớn hơn có phân khu
                    strs.length == 2 && (state == State.MISSING || customArea.getArea() < countryArea)
                            // các quốc gia không có phân khu chỉ vượt trội hơn những quốc gia lớn hơn không có phân khu
                            || strs.length == 1 && (state == State.MISSING && customArea.getArea() < countryArea)) {
                country = c;
                state = State.find(alpha2WithSubdivision);
                countryArea = customArea.getArea();
            }
        }
        way.setTag("country", country);
        way.setTag("country_state", state);

        if (countryRuleFactory != null) {
            CountryRule countryRule = countryRuleFactory.getCountryRule(country);
            if (countryRule != null)
                way.setTag("country_rule", countryRule);
        }

        // cũng thêm tất cả các khu vực tùy chỉnh như thẻ nhân tạo
        way.setTag("custom_areas", customAreas);
    }

    /**
     * Phương thức này được gọi cho mỗi đoạn mà cách OSM được chia thành trong lần thứ hai của {@link WaySegmentParser}.
     *
     * @param fromIndex một id số nguyên duy nhất cho nút đầu tiên của đoạn này
     * @param toIndex   một id số nguyên duy nhất cho nút cuối cùng của đoạn này
     * @param pointList tọa độ của đoạn này
     * @param way       cách OSM mà đoạn này được lấy từ
     * @param nodeTags  các thẻ nút của đoạn này. có một bản đồ thẻ cho mỗi điểm.
     */
    protected void addEdge(int fromIndex, int toIndex, PointList pointList, ReaderWay way, List<Map<String, Object>> nodeTags) {
        // kiểm tra hợp lệ
        if (fromIndex < 0 || toIndex < 0)
            throw new AssertionError("to hoặc from index không hợp lệ cho cạnh này " + fromIndex + "->" + toIndex + ", points:" + pointList);
        if (pointList.getDimension() != nodeAccess.getDimension())
            throw new AssertionError("Kích thước không khớp cho pointList so với nodeAccess " + pointList.getDimension() + " <-> " + nodeAccess.getDimension());
        if (pointList.size() != nodeTags.size())
            throw new AssertionError("phải có nhiều bản đồ thẻ nút như có điểm. node tags: " + nodeTags.size() + ", points: " + pointList.size());

        // todo: về nguyên tắc, nó phải có thể trì hoãn việc tính toán độ cao để chúng ta không cần phải lưu trữ
        // các độ cao trong quá trình nhập (tiết kiệm bộ nhớ trong thông tin cột trong quá trình nhập). cũng lưu ý rằng chúng ta đã phải
        // để làm một số loại xử lý độ cao (nội suy cầu và hầm trong lớp GraphHopper, có thể điều này có thể
        // đi cùng nhau

        if (pointList.is3D()) {
            // lấy mẫu điểm dọc theo các cạnh dài
            if (config.getLongEdgeSamplingDistance() < Double.MAX_VALUE)
                pointList = EdgeSampling.sample(pointList, config.getLongEdgeSamplingDistance(), distCalc, eleProvider);

            // làm mịn độ cao trước khi tính toán khoảng cách vì khoảng cách sẽ không chính xác nếu được tính sau
            if (config.getElevationSmoothing().equals("ramer"))
                EdgeElevationSmoothingRamer.smooth(pointList, config.getElevationSmoothingRamerMax());
            else if (config.getElevationSmoothing().equals("moving_average"))
                EdgeElevationSmoothingMovingAverage.smooth(pointList, config.getSmoothElevationAverageWindowSize());
            else if (!config.getElevationSmoothing().isEmpty())
                throw new AssertionError("Thuật toán làm mịn độ cao không được hỗ trợ: '" + config.getElevationSmoothing() + "'");
        }

        if (config.getMaxWayPointDistance() > 0 && pointList.size() > 2)
            simplifyAlgo.simplify(pointList);

        double distance = distCalc.calcDistance(pointList);

        if (distance < 0.001) {
            // Như điều tra cho thấy thường hai con đường nên đã giao nhau qua một điểm giống nhau
            // nhưng kết thúc ở hai điểm rất gần.
            zeroCounter++;
            distance = 0.001;
        }

        double maxDistance = (Integer.MAX_VALUE - 1) / 1000d;
        if (Double.isNaN(distance)) {
            LOGGER.warn("Lỗi trong OSM hoặc GraphHopper. Khoảng cách nút tháp bất hợp pháp " + distance + " reset thành 1m, osm way " + way.getId());
            distance = 1;
        }

        if (Double.isInfinite(distance) || distance > maxDistance) {
            // Quá lớn là rất hiếm và thường là việc gắn thẻ sai. Xem #435
            // vì vậy chúng ta có thể tránh sự phức tạp của việc chia cách cho bây giờ (cần các towernodes mới, chia hình học, v.v.)
            // Ví dụ, điều này xảy ra ở đây: https://www.openstreetmap.org/way/672506453 (Phà Cape Town - Tristan da Cunha)
            LOGGER.warn("Lỗi trong OSM hoặc GraphHopper. Khoảng cách nút tháp quá lớn " + distance + " reset thành giá trị lớn, osm way " + way.getId());
            distance = maxDistance;
        }

        setArtificialWayTags(pointList, way, distance, nodeTags);
        IntsRef relationFlags = getRelFlagsMap(way.getId());
        EdgeIteratorState edge = baseGraph.edge(fromIndex, toIndex).setDistance(distance);
        osmParsers.handleWayTags(edge.getEdge(), edgeIntAccess, way, relationFlags);
        List<KVStorage.KeyValue> list = way.getTag("key_values", Collections.emptyList());
        if (!list.isEmpty())
            edge.setKeyValues(list);

        // Nếu toàn bộ cách chỉ là điểm đầu tiên và cuối cùng, không lãng phí không gian lưu trữ hình học cách trống
        if (pointList.size() > 2) {
            // hình học chỉ bao gồm các nút cột, nhưng chúng tôi kiểm tra rằng các điểm đầu tiên và cuối cùng của pointList
            // bằng với tọa độ nút tháp
            checkCoordinates(fromIndex, pointList.get(0));
            checkCoordinates(toIndex, pointList.get(pointList.size() - 1));
            edge.setWayGeometry(pointList.shallowCopy(1, pointList.size() - 1, false));
        }

        checkDistance(edge);
        restrictedWaysToEdgesMap.putIfReserved(way.getId(), edge.getEdge());
    }

    private void checkCoordinates(int nodeIndex, GHPoint point) {
        final double tolerance = 1.e-6;
        if (Math.abs(nodeAccess.getLat(nodeIndex) - point.getLat()) > tolerance || Math.abs(nodeAccess.getLon(nodeIndex) - point.getLon()) > tolerance)
            throw new IllegalStateException("Tọa độ đáng ngờ cho nút " + nodeIndex + ": (" + nodeAccess.getLat(nodeIndex) + "," + nodeAccess.getLon(nodeIndex) + ") so với (" + point + ")");
    }

    private void checkDistance(EdgeIteratorState edge) {
        final double tolerance = 1;
        final double edgeDistance = edge.getDistance();
        final double geometryDistance = distCalc.calcDistance(edge.fetchWayGeometry(FetchMode.ALL));
        if (Double.isInfinite(edgeDistance))
            throw new IllegalStateException("Khoảng cách cạnh vô hạn không bao giờ xảy ra, vì chúng tôi được cho là giới hạn mỗi khoảng cách đến khoảng cách tối đa mà chúng tôi có thể lưu trữ, #435");
        else if (edgeDistance > 2_000_000)
            LOGGER.warn("Phát hiện cạnh rất dài: " + edge + " dist: " + edgeDistance);
        else if (Math.abs(edgeDistance - geometryDistance) > tolerance)
            throw new IllegalStateException("Khoảng cách đáng ngờ cho cạnh: " + edge + " " + edgeDistance + " so với " + geometryDistance
                    + ", sự khác biệt: " + (edgeDistance - geometryDistance));
    }

    /**
     * This method is called for each way during the second pass and before the way is split into edges.
     * We currently use it to parse road names and calculate the distance of a way to determine the speed based on
     * the duration tag when it is present. The latter cannot be done on a per-edge basis, because the duration tag
     * refers to the duration of the entire way.
     */
    protected void preprocessWay(ReaderWay way, WaySegmentParser.CoordinateSupplier coordinateSupplier) {
        // storing the road name does not yet depend on the flagEncoder so manage it directly
        List<KVStorage.KeyValue> list = new ArrayList<>();
        if (config.isParseWayNames()) {
            // http://wiki.openstreetmap.org/wiki/Key:name
            String name = "";
            if (!config.getPreferredLanguage().isEmpty())
                name = fixWayName(way.getTag("name:" + config.getPreferredLanguage()));
            if (name.isEmpty())
                name = fixWayName(way.getTag("name"));
            if (!name.isEmpty())
                list.add(new KVStorage.KeyValue(STREET_NAME, name));

            // http://wiki.openstreetmap.org/wiki/Key:ref
            String refName = fixWayName(way.getTag("ref"));
            if (!refName.isEmpty())
                list.add(new KVStorage.KeyValue(STREET_REF, refName));

            if (way.hasTag("destination:ref")) {
                list.add(new KVStorage.KeyValue(STREET_DESTINATION_REF, fixWayName(way.getTag("destination:ref"))));
            } else {
                if (way.hasTag("destination:ref:forward"))
                    list.add(new KVStorage.KeyValue(STREET_DESTINATION_REF, fixWayName(way.getTag("destination:ref:forward")), true, false));
                if (way.hasTag("destination:ref:backward"))
                    list.add(new KVStorage.KeyValue(STREET_DESTINATION_REF, fixWayName(way.getTag("destination:ref:backward")), false, true));
            }
            if (way.hasTag("destination")) {
                list.add(new KVStorage.KeyValue(STREET_DESTINATION, fixWayName(way.getTag("destination"))));
            } else {
                if (way.hasTag("destination:forward"))
                    list.add(new KVStorage.KeyValue(STREET_DESTINATION, fixWayName(way.getTag("destination:forward")), true, false));
                if (way.hasTag("destination:backward"))
                    list.add(new KVStorage.KeyValue(STREET_DESTINATION, fixWayName(way.getTag("destination:backward")), false, true));
            }
        }
        way.setTag("key_values", list);

        if (!isCalculateWayDistance(way))
            return;

        double distance = calcDistance(way, coordinateSupplier);
        if (Double.isNaN(distance)) {
            // Some nodes were missing, and we cannot determine the distance. This can happen when ways are only
            // included partially in an OSM extract. In this case we cannot calculate the speed either, so we return.
            LOGGER.warn("Could not determine distance for OSM way: " + way.getId());
            return;
        }
        way.setTag("way_distance", distance);

        // For ways with a duration tag we determine the average speed. This is needed for e.g. ferry routes, because
        // the duration tag is only valid for the entire way, and it would be wrong to use it after splitting the way
        // into edges.
        String durationTag = way.getTag("duration");
        if (durationTag == null) {
            // no duration tag -> we cannot derive speed. happens very frequently for short ferries, but also for some long ones, see: #2532
            if (isFerry(way) && distance > 500_000)
                OSM_WARNING_LOGGER.warn("Long ferry OSM way without duration tag: " + way.getId() + ", distance: " + Math.round(distance / 1000.0) + " km");
            return;
        }
        long durationInSeconds;
        try {
            durationInSeconds = OSMReaderUtility.parseDuration(durationTag);
        } catch (Exception e) {
            OSM_WARNING_LOGGER.warn("Could not parse duration tag '" + durationTag + "' in OSM way: " + way.getId());
            return;
        }

        double speedInKmPerHour = distance / 1000 / (durationInSeconds / 60.0 / 60.0);
        if (speedInKmPerHour < 0.1d) {
            // Often there are mapping errors like duration=30:00 (30h) instead of duration=00:30 (30min). In this case we
            // ignore the duration tag. If no such cases show up anymore, because they were fixed, maybe raise the limit to find some more.
            OSM_WARNING_LOGGER.warn("Unrealistic low speed calculated from duration. Maybe the duration is too long, or it is applied to a way that only represents a part of the connection? OSM way: "
                    + way.getId() + ". duration=" + durationTag + " (= " + Math.round(durationInSeconds / 60.0) +
                    " minutes), distance=" + distance + " m");
            return;
        }
        // tag will be present if 1) isCalculateWayDistance was true for this way, 2) no OSM nodes were missing
        // such that the distance could actually be calculated, 3) there was a duration tag we could parse, and 4) the
        // derived speed was not unrealistically slow.
        way.setTag("speed_from_duration", speedInKmPerHour);
    }

    static String fixWayName(String str) {
        if (str == null)
            return "";
        // the KVStorage does not accept too long strings -> Helper.cutStringForKV
        return KVStorage.cutString(WAY_NAME_PATTERN.matcher(str).replaceAll(", "));
    }

    /**
     * @return the distance of the given way or NaN if some nodes were missing
     */
    private double calcDistance(ReaderWay way, WaySegmentParser.CoordinateSupplier coordinateSupplier) {
        LongArrayList nodes = way.getNodes();
        // every way has at least two nodes according to our acceptWay function
        GHPoint3D prevPoint = coordinateSupplier.getCoordinate(nodes.get(0));
        if (prevPoint == null)
            return Double.NaN;
        boolean is3D = !Double.isNaN(prevPoint.ele);
        double distance = 0;
        for (int i = 1; i < nodes.size(); i++) {
            GHPoint3D point = coordinateSupplier.getCoordinate(nodes.get(i));
            if (point == null)
                return Double.NaN;
            if (Double.isNaN(point.ele) == is3D)
                throw new IllegalStateException("There should be elevation data for either all points or no points at all. OSM way: " + way.getId());
            distance += is3D
                    ? distCalc.calcDist3D(prevPoint.lat, prevPoint.lon, prevPoint.ele, point.lat, point.lon, point.ele)
                    : distCalc.calcDist(prevPoint.lat, prevPoint.lon, point.lat, point.lon);
            prevPoint = point;
        }
        return distance;
    }

    /**
     * This method is called for each relation during the first pass of {@link WaySegmentParser}
     */
    protected void preprocessRelations(ReaderRelation relation) {
        if (!relation.isMetaRelation() && relation.hasTag("type", "route")) {
            // we keep track of all route relations, so they are available when we create edges later
            for (ReaderRelation.Member member : relation.getMembers()) {
                if (member.getType() != ReaderElement.Type.WAY)
                    continue;
                IntsRef oldRelationFlags = getRelFlagsMap(member.getRef());
                IntsRef newRelationFlags = osmParsers.handleRelationTags(relation, oldRelationFlags);
                putRelFlagsMap(member.getRef(), newRelationFlags);
            }
        }

        Arrays.stream(RestrictionConverter.getRestrictedWayIds(relation))
                .forEach(restrictedWaysToEdgesMap::reserve);
    }

    /**
     * This method is called for each relation during the second pass of {@link WaySegmentParser}
     * We use it to save the relations and process them afterwards.
     */
    protected void processRelation(ReaderRelation relation, LongToIntFunction getIdForOSMNodeId) {
        if (turnCostStorage != null)
            if (RestrictionConverter.isTurnRestriction(relation)) {
                long osmViaNode = RestrictionConverter.getViaNodeIfViaNodeRestriction(relation);
                if (osmViaNode >= 0) {
                    int viaNode = getIdForOSMNodeId.applyAsInt(osmViaNode);
                    // only include the restriction if the corresponding node wasn't excluded
                    if (viaNode >= 0) {
                        relation.setTag("graphhopper:via_node", viaNode);
                        restrictionRelations.add(relation);
                    }
                } else
                    // not a via-node restriction -> simply add it as is
                    restrictionRelations.add(relation);
            }
    }

    private void addRestrictionsToGraph() {
        // The OSM restriction format is explained here: https://wiki.openstreetmap.org/wiki/Relation:restriction
        List<Triple<ReaderRelation, GraphRestriction, RestrictionMembers>> restrictions = new ArrayList<>(restrictionRelations.size());
        for (ReaderRelation restrictionRelation : restrictionRelations) {
            try {
                // convert the OSM relation topology to the graph representation. this only needs to be done once for all
                // vehicle types (we also want to print warnings only once)
                restrictions.add(RestrictionConverter.convert(restrictionRelation, baseGraph, restrictedWaysToEdgesMap::getEdges));
            } catch (OSMRestrictionException e) {
                warnOfRestriction(restrictionRelation, e);
            }
        }
        // The restriction type depends on the vehicle, or at least not all restrictions affect every vehicle type.
        // We handle the restrictions for one vehicle after another.
        for (RestrictionTagParser restrictionTagParser : osmParsers.getRestrictionTagParsers()) {
            LongSet viaWaysUsedByOnlyRestrictions = new LongHashSet();
            List<Pair<GraphRestriction, RestrictionType>> restrictionsWithType = new ArrayList<>(restrictions.size());
            for (Triple<ReaderRelation, GraphRestriction, RestrictionMembers> r : restrictions) {
                if (r.second == null)
                    // this relation was found to be invalid by another restriction tag parser already
                    continue;
                try {
                    RestrictionTagParser.Result res = restrictionTagParser.parseRestrictionTags(r.first.getTags());
                    if (res == null)
                        // this relation is ignored by the current restriction tag parser
                        continue;
                    RestrictionConverter.checkIfCompatibleWithRestriction(r.second, res.getRestriction());
                    // we ignore 'only' via-way restrictions that share the same via way, because these would require adding
                    // multiple artificial edges, see here: https://github.com/graphhopper/graphhopper/pull/2689#issuecomment-1306769694
                    if (r.second.isViaWayRestriction() && res.getRestrictionType() == RestrictionType.ONLY)
                        for (LongCursor viaWay : r.third.getViaWays())
                            if (!viaWaysUsedByOnlyRestrictions.add(viaWay.value))
                                throw new OSMRestrictionException("has a member with role 'via' that is also used as 'via' member by another 'only' restriction. GraphHopper cannot handle this.");
                    restrictionsWithType.add(new Pair<>(r.second, res.getRestrictionType()));
                } catch (OSMRestrictionException e) {
                    warnOfRestriction(r.first, e);
                    // we only want to print a warning once for each restriction relation, so we make sure this
                    // restriction is ignored for the other vehicles
                    r.second = null;
                }
            }
            restrictionSetter.setRestrictions(restrictionsWithType, restrictionTagParser.getTurnRestrictionEnc());
        }
    }

    public IntIntMap getArtificialEdgesByEdges() {
        return restrictionSetter.getArtificialEdgesByEdges();
    }

    private static void warnOfRestriction(ReaderRelation restrictionRelation, OSMRestrictionException e) {
        // we do not log exceptions with an empty message
        if (!e.isWithoutWarning()) {
            restrictionRelation.getTags().remove("graphhopper:via_node");
            List<String> members = restrictionRelation.getMembers().stream().map(m -> m.getRole() + " " + m.getType().toString().toLowerCase() + " " + m.getRef()).collect(Collectors.toList());
            OSM_WARNING_LOGGER.warn("Restriction relation " + restrictionRelation.getId() + " " + e.getMessage() + ". tags: " + restrictionRelation.getTags() + ", members: " + members + ". Relation ignored.");
        }
    }

    private void releaseEverythingExceptRestrictionData() {
        eleProvider.release();
        osmWayIdToRelationFlagsMap = null;
    }

    private void releaseRestrictionData() {
        restrictedWaysToEdgesMap = null;
        restrictionRelations = null;
    }

    IntsRef getRelFlagsMap(long osmId) {
        long relFlagsAsLong = osmWayIdToRelationFlagsMap.get(osmId);
        tempRelFlags.ints[0] = (int) relFlagsAsLong;
        tempRelFlags.ints[1] = (int) (relFlagsAsLong >> 32);
        return tempRelFlags;
    }

    void putRelFlagsMap(long osmId, IntsRef relFlags) {
        long relFlagsAsLong = ((long) relFlags.ints[1] << 32) | (relFlags.ints[0] & 0xFFFFFFFFL);
        osmWayIdToRelationFlagsMap.put(osmId, relFlagsAsLong);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
