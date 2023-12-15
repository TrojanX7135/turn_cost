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

@Path("deleteTag")
@Produces(MediaType.APPLICATION_JSON)
public class DeleteTagResource {
    private GraphHopper graphHopper;
    public static class KeyValues {
        public Map<String, String> KeyValuesMap;
    }

    @Inject
    public DeleteTagResource(GraphHopper graphHopper)
    {
        this.graphHopper = graphHopper;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public KeyValues doGet(
            @QueryParam("OSMId") @DefaultValue("0")String OSMId,
            @QueryParam("key") @DefaultValue("None") String key)
    {
        final KeyValues key_value = new KeyValues();
        key_value.KeyValuesMap = new LinkedHashMap<>();
        ReaderWay way= this.graphHopper.getReader().getWaysegment().getWayFromOSMId(parseLong(OSMId));
        if(way != null)
        {
            List<String> keysList = new ArrayList<>(way.getTags().keySet());
            List<Integer> listEdge = this.graphHopper.getReader().getEgdeFromWay(parseLong(OSMId));
            if(key.contains("None"))
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
                    System.out.println("Tag not Exists");
                }
                else
                {
                    keysList.remove(key);
                    way.removeTag(key);
                    System.out.println("Delete Successful");
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
