package com.graphhopper.resources;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.Constants;
import com.graphhopper.util.PMap;
import org.locationtech.jts.geom.Envelope;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Long.parseLong;

@Path("updateTag")
@Produces(MediaType.APPLICATION_JSON)
public class UpdateTagResource {
    private GraphHopper graphHopper;
    public static class KeyValues {
        public Map<String, String> KeyValuesMap;
    }

    @Inject
    public UpdateTagResource(GraphHopper graphHopper)
    {
        this.graphHopper = graphHopper;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public KeyValues doGet(
            @QueryParam("OSMId") @DefaultValue("0")String OSMId,
            @QueryParam("key") @DefaultValue("None") String key,
            @QueryParam("value") @DefaultValue("None") String value)
    {
        final KeyValues key_value = new KeyValues();
        key_value.KeyValuesMap = new LinkedHashMap<>();
        ReaderWay way= this.graphHopper.getReader().getWaysegment().getWayFromOSMId(parseLong(OSMId));
        if(way != null)
        {
            List<String> keysList = new ArrayList<>(way.getTags().keySet());

            List<Integer> listEdge = this.graphHopper.getReader().getEgdeFromWay(parseLong(OSMId));
            if(key.contains("None") && value.contains("None"))
            {
                for (String s : keysList) {
                    try{
                        way.getTag(s);
                    }
                    catch(Exception e)
                    {
                        continue;
                    }
                    key_value.KeyValuesMap.put(s, way.getTag(s));
                    System.out.println(s);
                }
            }
            else
            {
                if(!checkExists(key,keysList))
                {
                    keysList.add(key);
                    way.setTag(key,value);
                    System.out.print("New key: ");
                    System.out.println(key);
                    System.out.print("New value: ");
                    System.out.println(way.getTag(key));
                }
                else
                {
                    System.out.print("Old value: ");
                    System.out.println(way.getTag(key));
                    way.setTag(key,value);
                    System.out.print("New value: ");
                    System.out.println(way.getTag(key));
                }
                IntsRef relationFlags = this.graphHopper.getReader().getRelFlagsMap(parseLong(OSMId));
                for (Integer integer : listEdge)
                {
                    this.graphHopper.getOSMParsers().handleWayTags(integer, this.graphHopper.getBaseGraph().createEdgeIntAccess(), way, relationFlags);
                }
            }
        }
        return key_value;
    }

    private boolean checkExists(String Querykey, List<String> listKey)
    {
        for(String key : listKey)
        {
            if(key.equals(Querykey)) return true;
        }
        return false;
    }
}
