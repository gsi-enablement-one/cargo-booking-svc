package com.cargotracker.booking.application.internal.outboundservices.acl;

import com.cargotracker.booking.domain.model.valueobjects.CargoItinerary;
import com.cargotracker.booking.domain.model.valueobjects.Leg;
import com.cargotracker.booking.domain.model.valueobjects.RouteSpecification;
import com.cargotracker.shareddomain.model.TransitEdge;
import com.cargotracker.shareddomain.model.TransitPath;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anti Corruption Service Class
 */
@Service
public class ExternalCargoRoutingService {
	@Value("${spring.profiles.active:localhost}")
	private String activeProfile;
	
    /**
     * The Booking Bounded Context makes an external call to the Routing Service of the Routing Bounded Context to
     * fetch the Optimal Itinerary for a Cargo based on the Route Specification
     * @param routeSpecification
     * @return
     */
    public CargoItinerary fetchRouteForSpecification(RouteSpecification routeSpecification){
        RestTemplate restTemplate = new RestTemplate();
        Map<String,Object> params = new HashMap<>();
        params.put("origin",routeSpecification.getOrigin().getUnLocCode());
        params.put("destination",routeSpecification.getDestination().getUnLocCode());
        params.put("arrivalDeadline",routeSpecification.getArrivalDeadline().toString());
        
        String host = "routing.routing";
        if (activeProfile.equals("local")) host = "localhost";
        
        TransitPath transitPath = restTemplate.getForObject("http://" + host + ":8003/cargorouting/optimalRoute?origin=&destination=&deadline=",
                    TransitPath.class,params);


        List<Leg> legs = new ArrayList<>(transitPath.getTransitEdges().size());
        for (TransitEdge edge : transitPath.getTransitEdges()) {
            legs.add(toLeg(edge));
        }

        return new CargoItinerary(legs);

    }

    /**
     * Anti-corruption layer conversion method from the routing service's domain model (TransitEdges)
     * to the domain model recognized by the Booking Bounded Context (Legs)
     * @param edge
     * @return
     */
    private Leg toLeg(TransitEdge edge) {
        return new Leg(edge.getVoyageNumber(), edge.getFromUnLocode(), edge.getToUnLocode(),
                       edge.getFromDate(), edge.getToDate());
        }
}
