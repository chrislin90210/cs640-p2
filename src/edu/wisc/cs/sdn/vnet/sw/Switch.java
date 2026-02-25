package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;

import java.util.Map;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device {
	private class TableEntry<Iface, Date> {
		private Iface iface;
		private Date lastUsed;

		public TableEntry(Iface iface, Date lastUsed) {
			this.iface = iface;
			this.lastUsed = lastUsed;
		}

		public Iface getIface() {
			return iface;
		}

		public Date getLastUsed() {

		}

		public void setIface() {

		}

		public void setLastUsed() {

		}
	}

	}/**
		 * Creates a router for a specific host.
		 * 
		 * @param host hostname for the router
		 */
	Map<MACAddress,

	public Switch(String host, DumpFile logfile) {
		super(host, logfile);
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

		/********************************************************************/
		/* TODO: Handle packets */
		MACAddress dst = etherPacket.getDestinationMAC();
		MACAddress src = etherPacket.getSourceMAC();

		sendPacket(etherPacket, inIface);
		for (Map.Entry<String, Iface> entry : interfaces.entrySet()) {
			sendPacket(entry.getValue());
		}
	}

	/********************************************************************/
}}
