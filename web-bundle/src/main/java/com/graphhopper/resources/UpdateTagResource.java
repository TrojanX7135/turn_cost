package com.graphhopper.resources;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.storage.IntsRef;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Long.parseLong;

@Path("updateTag")
@Produces(MediaType.APPLICATION_JSON)
public class UpdateTagResource {
    private GraphHopper graphHopper;
    @Inject
    public UpdateTagResource(GraphHopper graphHopper)
    {
        this.graphHopper = graphHopper;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/gpx+xml"})
    public String doGet(
            @QueryParam("OSMId") @DefaultValue("0")String OSMId,
            @QueryParam("key") @DefaultValue("None") String key,
            @QueryParam("value") @DefaultValue("None") String value)
    {
        ReaderWay way= this.graphHopper.getReader().getWaysegment().getWayFromOSMId(parseLong(OSMId));
        if(way != null)
        {
            List<Integer> listEdge = this.graphHopper.getReader().getEgdeFromWay(parseLong(OSMId));
            if(key.contains("None") && value.contains("None"))
            {
                List<String> keysList = new ArrayList<>(way.getTags().keySet());
                for (String s : keysList) {
                    if(s.contains("key_values")) break;
                    System.out.println(s);
                }
            }
            else
            {
                System.out.print("Old value: ");
                System.out.println(way.getTag(key));
                way.setTag(key,value);
                IntsRef relationFlags = this.graphHopper.getReader().getRelFlagsMap(parseLong(OSMId));
                for (Integer integer : listEdge)
                {
                    this.graphHopper.getOSMParsers().handleWayTags(integer, this.graphHopper.getBaseGraph().createEdgeIntAccess(), way, relationFlags);
                }
                System.out.print("New value: ");
                System.out.println(way.getTag(key));
                return "OKE";
            }
        }
        return "FAIL";
    }
}
