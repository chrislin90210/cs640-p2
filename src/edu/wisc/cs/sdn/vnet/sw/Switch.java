package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;

import java.util.HashMap;
import java.util.Map;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device {

    private final Long MAC_LEARNING_TIMEOUT_MS = new Long(15000);

    private class TableEntry {
        private Iface iface;
        private Long lastUsed;

        public TableEntry(Iface iface, Long lastUsed) {
            this.iface = iface;
            this.lastUsed = lastUsed;
        }

        public Iface getIface() {
            return iface;
        }

        public Long getLastUsed() {
            return this.lastUsed;
        }

        public void setIface(Iface iface) {
            this.iface = iface;
        }

        public void setLastUsed(Long lastUsed) {
            this.lastUsed = lastUsed;
        }
    }

    /**
     * Creates a router for a specific host.
     * 
     * @param host hostname for the router
     */
    Map<MACAddress, TableEntry> forwardingTable = new HashMap<>();

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
        Long currTime = System.currentTimeMillis();
        forwardingTable.entrySet()
                .removeIf(entry -> currTime - entry.getValue().getLastUsed() >= MAC_LEARNING_TIMEOUT_MS);

        /********************************************************************/
        /* TODO: Handle packets */
        MACAddress dst = etherPacket.getDestinationMAC();
        MACAddress src = etherPacket.getSourceMAC();
        if (forwardingTable.containsKey(src)) {
            // update fwding tbl
            TableEntry srcEntry = forwardingTable.get(src);
            srcEntry.setLastUsed(System.currentTimeMillis());
            srcEntry.setIface(inIface);
        } else {
            TableEntry entry = new TableEntry(inIface, System.currentTimeMillis());
            forwardingTable.put(src, entry);
        }
        if (forwardingTable.containsKey(dst)) {
            TableEntry dstTableEntry = forwardingTable.get(dst);
            dstTableEntry.setLastUsed(System.currentTimeMillis());
            Iface dstInterface = dstTableEntry.getIface();
            sendPacket(etherPacket, dstInterface);
            return;
        }

        // broadcast
        for (Map.Entry<String, Iface> entry : interfaces.entrySet()) {
            if (entry.getValue().equals(inIface))
                continue;
            sendPacket(etherPacket, entry.getValue());
        }

    }

    /********************************************************************/
}
