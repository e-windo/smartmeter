package uk.ac.imperial.smartmeter.allocator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.imperial.smartmeter.comparators.requirementPrioComparator;
import uk.ac.imperial.smartmeter.res.ArraySet;
import uk.ac.imperial.smartmeter.res.EleGenConglomerate;
import uk.ac.imperial.smartmeter.res.ElectricityRequirement;
import uk.ac.imperial.smartmeter.res.ElectricityTicket;

public class TicketAllocator {
	private Map<ElectricityRequirement, ArrayList<QuantumNode>> reqMap;
	private CalendarQueue queue;
	private ArraySet<UserAgent> usrArray;
	private Map<UserAgent, ArraySet<ElectricityTicket>> tickets;
	private RescherArbiter arbiter;
	private EleGenConglomerate conglom;
	private Date startDate;
	private Map<UserAgent, Double> rankings;
	private Map<UserAgent, Integer> indexes; 
	private Map<UserAgent, Boolean> userFinished; 
	
	public TicketAllocator(ArraySet<UserAgent> users, Date d)
	{
		reqMap = new HashMap<ElectricityRequirement, ArrayList<QuantumNode>>();
		usrArray = new ArraySet<UserAgent>();
		arbiter = new RescherArbiter();
		tickets = new HashMap<UserAgent, ArraySet<ElectricityTicket>>();
		conglom = new EleGenConglomerate();
		startDate = d;
		for (UserAgent u : users)
		{
			addUser(u);
		}
		

		queue = new CalendarQueue(conglom,startDate);
		populateReqMap();
	}
	private void populateReqMap()
	{
		for (UserAgent u : usrArray)
		{
		for (ElectricityRequirement e: u.getReqs())
		{
			reqMap.put(e, queue.findIntersectingNodes(e));
		}
		}
	}
	private void addUser(UserAgent u)
	{
		tickets.put(u, new ArraySet<ElectricityTicket>());
		conglom.addGen(u.getGeneratedPower());
		usrArray.add(u);
	}
	private ElectricityTicket generateTicket(ElectricityRequirement e)
	{
		return new ElectricityTicket(e.getStartTime(), e.getEndTime(), e.getMaxConsumption(), e.getUserID());
	}
	public ArraySet<ElectricityTicket> getTicketsOfUser(UserAgent u)
	{
		return tickets.get(u);
	}
	private UserAgent findMaxAgent(Map<UserAgent,Double> m)
	{
		UserAgent maxAgt = null;
		Double max = -1.;
		for (Entry<UserAgent, Double> u : m.entrySet())
		{
			if (u.getValue() > max)
			{
				maxAgt = u.getKey();
				max = u.getValue();
			}
		}
		return maxAgt;
	}
	private Boolean updateInterferedNodes(ElectricityRequirement e, ArrayList<QuantumNode> nodes)
	{
		Boolean viable = true; //until proved otherwise;
		Double requirementMagnitude = e.getMaxConsumption();
		for (QuantumNode n : nodes)
		{
			viable &= (n.getCapacity() >= requirementMagnitude);
		}
		
		if (viable)
		{
			for (QuantumNode n : nodes)
			{
				n.addReq(e);
			}
		}
		return viable;
	}
	
	private Boolean processRequirement(UserAgent u, ElectricityRequirement req)
	{
		if (updateInterferedNodes(req, reqMap.get(req))) { 
			ElectricityTicket ticket = generateTicket(req);
			tickets.get(u).add(ticket);
		}
		
		return false;
		
	}
	public Map<UserAgent, ArraySet<ElectricityTicket>> calculateTickets()
	{
		rankings = arbiter.getWeighting(usrArray);
		indexes = new HashMap<UserAgent, Integer>();
		userFinished = new HashMap<UserAgent, Boolean>();
		
		Boolean finished = false;
		
		for (UserAgent u : usrArray)
		{
			Collections.sort(u.getReqs(), new requirementPrioComparator());
			indexes.put(u, 0);
			userFinished.put(u,false);
		}
		
		while (!finished) {
			/*finds the highest priority agent, and tries to create a ticket for the highest priority requirement they have */
			finished = true;
			UserAgent max = findMaxAgent(rankings);
			processRequirement(max, max.getReq(indexes.get(max)));
			
			/* attempts to move on to the next requirement in the available set */
			if (indexes.get(max) < max.getReqs().getSize() - 1) {
				indexes.put(max, indexes.get(max) + 1);
				rankings.put(max, rankings.get(max)-1);
			}
			else
			{
		    /* there are no further requirements for that user */
				userFinished.put(max, true);
				rankings.put(max, 0.);
			}
			for (Boolean b : userFinished.values())
			{
				/*we are finished if there are no requirements left to be addressed*/
				finished &= b;
			}
		}
		return tickets;
		
	}
}