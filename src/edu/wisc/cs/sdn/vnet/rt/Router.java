package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.Map;
import java.util.Collection;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.MACAddress;

import java.nio.ByteBuffer;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device {
	/** Routing table for the router */
	private RouteTable routeTable;

	/** ARP cache for the router */
	private ArpCache arpCache;

	/**
	 * Creates a router for a specific host.
	 * 
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile) {
		super(host, logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}

	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable() {
		return this.routeTable;
	}

	/**
	 * Load a new routing table from a file.
	 * 
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile) {
		if (!routeTable.load(routeTableFile, this)) {
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}

		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}

	/**
	 * Load a new ARP cache from a file.
	 * 
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile) {
		if (!arpCache.load(arpCacheFile)) {
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}

		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * 
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface     the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface) {
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));

		// check if frame contains an IPv4 packet
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
			return; // drop the packet

		// get the payload
		IPv4 payload = (IPv4) etherPacket.getPayload();
		// get header length
		byte headerLength = payload.getHeaderLength();

		// serialize header
		byte[] data = new byte[headerLength * 4]; // since header length is in 4-byte units
		ByteBuffer byteBuffer = ByteBuffer.wrap(data);
		byteBuffer.put((byte) (((payload.getVersion() & 0xf) << 4) | (headerLength & 0xf)));
		byteBuffer.put(payload.getDiffServ());
		byteBuffer.putShort(payload.getTotalLength());
		byteBuffer.putShort(payload.getIdentification());
		byteBuffer.putShort((short) (((payload.getFlags() & 0x7) << 13) | (payload.getFragmentOffset() & 0x1fff)));
		byteBuffer.put(payload.getTtl());
		byteBuffer.put(payload.getProtocol());
		// in place of checksum, place 0
		byteBuffer.putShort((short) 0);
		byteBuffer.putInt(payload.getSourceAddress());
		byteBuffer.putInt(payload.getDestinationAddress());
		if (payload.getOptions() != null)
			byteBuffer.put(payload.getOptions());

		// validate checksum
		short recomputedChecksum = recomputeChecksum(byteBuffer, headerLength);
		if (recomputedChecksum != payload.getChecksum())
			return; // drop the packet

		// decrement the time-to-live field of the packet and drop if dead
		payload.setTtl(payload.getTtl() - 1);
		if (payload.getTtl() == 0)
			return; // drop the packet

		// since TTL is changed, reset checksum
		payload.resetChecksum();
		payload.serialize();

		int destinationAddress = payload.getDestinationAddress();
		// get all interfaces in this router
		Map<String, Iface> interfacesMap = getInterfaces();
		Collection<Iface> interfaces = interfacesMap.values();
		// drop packet if it is destined to this router
		for (Iface iface : interfaces)
			if (iface.getIpAddress() == destinationAddress)
				return; // drop packet

		// find a valid route entry in the route table for forwarding
		RouteEntry routeEntry = routeTable.lookup(destinationAddress);
		if (routeEntry == null)
			return; // drop the packet

		// look up the ARP Cache
		int nextHopAddress = routeEntry.getGatewayAddress() == 0 ? routeEntry.getDestinationAddress()
				: routeEntry.getGatewayAddress();
		ArpEntry arpEntry = arpCache.lookup(nextHopAddress);

		// drop packet if there is no entry in the ARP Cache
		if (arpEntry == null)
			return; // drop packet

		// set destination MAC address of the ethernet packet to the looked up MAC
		// address
		etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());
		// set source MAC address of the ethernet packet to that of the outgoing
		// interface
		etherPacket.setSourceMACAddress(routeEntry.getInterface().getMacAddress().toBytes());

		// send packet
		sendPacket(etherPacket, routeEntry.getInterface());
	}

	/**
	 * Compute the Checksum in the header of an IP Packet
	 * 
	 * @param ByteBuffer   the ByteBuffer containing serialized fields of the IP
	 *                     Header
	 * @param headerLength the length of the header in 4-byte units
	 * @return the computed checksum
	 */
	private short recomputeChecksum(ByteBuffer byteBuffer, byte headerLength) {
		// rewind the buffer
		byteBuffer.rewind();
		// checksum is calculated on 4-byte chunks, hence we use 'int'
		int accumulation = 0;
		// compute checksum
		for (int i = 0; i < headerLength * 2; ++i)
			accumulation += 0xffff & byteBuffer.getShort();
		accumulation = ((accumulation >> 16) & 0xffff)
				+ (accumulation & 0xffff);
		short computedChecksum = (short) (~accumulation & 0xffff);

		return computedChecksum;
	}
}
