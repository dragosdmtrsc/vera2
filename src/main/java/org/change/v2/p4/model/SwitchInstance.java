package org.change.v2.p4.model;

import org.change.v2.p4.model.actions.P4Action;
import org.change.v2.p4.model.actions.P4ActionCall;
import org.change.v2.p4.model.actions.P4ParameterInstance;
import org.change.v2.p4.model.table.MatchKind;
import org.change.v2.p4.model.table.TableMatch;
import org.change.v2.util.conversion.RepresentationConversion;
import scala.Int;
import sun.net.util.IPAddressUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by dragos on 31.08.2017.
 */
public class SwitchInstance implements ISwitchInstance {
    private Switch switchSpec;
    private String name;

    private Map<String, List<FlowInstance>> flowInstanceMap = new HashMap<String, List<FlowInstance>>();
    private Map<String, P4ActionCall> defaultActionMap = new HashMap<String, P4ActionCall>();

    @Override
    public Map<Integer, Integer> getCloneSpec2EgressSpec() {
        return cloneSpec2EgressSpec;
    }

    private Map<Integer, Integer> cloneSpec2EgressSpec = new HashMap<Integer, Integer>();

    public List<FlowInstance> flowInstanceIterator(String perTable) {
        if (flowInstanceMap.containsKey(perTable))
            return flowInstanceMap.get(perTable);
        else
            return new ArrayList<FlowInstance>();
    }

    public P4ActionCall getDefaultAction(String perTable) {
        if (defaultActionMap.containsKey(perTable))
            return defaultActionMap.get(perTable);
        return null;
    }

    public SwitchInstance setDefaultAction(String perTable, P4ActionCall call) {
        this.defaultActionMap.put(perTable, call);
        return this;
    }

    public Iterable<String> getDeclaredTables() {
        return flowInstanceMap.keySet();
    }

    private Map<Integer, String> ifaceSpec = new HashMap<Integer, String>();

    public SwitchInstance add(FlowInstance flowInstance) {
        if (!flowInstanceMap.containsKey(flowInstance.getTable())) {
            flowInstanceMap.put(flowInstance.getTable(), new ArrayList<FlowInstance>());
        }
        flowInstanceMap.get(flowInstance.getTable()).add(flowInstance);
        return this;
    }

    @Override
    public Map<Integer, String> getIfaceSpec() {
        return ifaceSpec;
    }

    /**
     * This happens only when the switch is started. For convenience,
     * have this param passed to the constructor
     * @param ifaceSpec - map between integer port numbers and interface names
     */
    public SwitchInstance(String name, Switch switchSpec, Map<Integer, String> ifaceSpec) {
        this.ifaceSpec = ifaceSpec;
        this.name = name;
        this.switchSpec = switchSpec;
    }

    public Switch getSwitchSpec() {
        return switchSpec;
    }

    @Override
    public String getName() {
        return name;
    }

    public static SwitchInstance fromP4AndDataplane(String p4File, String dataplane, List<String> ifaces) throws IOException {
        File f = new File(p4File);
        Map<Integer, String> mapped = new HashMap<Integer, String>();
        int i = 0;
        for (String s : ifaces) {
            mapped.put(i++, s);
        }
        return fromP4AndDataplane(p4File, dataplane, p4File, mapped);
    }
    public static SwitchInstance fromP4AndDataplane(String p4File,
                                                    String dataplane,
                                                    String name,
                                                    List<String> ifaces) throws IOException {
        Map<Integer, String> mapped = new HashMap<Integer, String>();
        int i = 0;
        for (String s : ifaces) {
            mapped.put(i++, s);
        }
        return fromP4AndDataplane(p4File, dataplane, name, mapped);
    }

    public static SwitchInstance fromP4AndDataplane(String p4File,
                                                    String dataplane,
                                                    String name,
                                                    Map<Integer, String> ifaces) throws IOException {
        Switch sw = Switch.fromFile(p4File);
        Map<Integer, String> mapped = new HashMap<Integer, String>();
        mapped.putAll(ifaces);

        String crt = null;
        SwitchInstance switchInstance = new SwitchInstance(name, sw, mapped);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dataplane)));
        return populateSwitchInstance(br, sw, switchInstance);
    }

    public static SwitchInstance populateSwitchInstance(String dataplane , Switch sw, SwitchInstance switchInstance) throws IOException {
        return populateSwitchInstance(new FileInputStream(dataplane), sw, switchInstance);
    }

    public static SwitchInstance populateSwitchInstance(InputStream is , Switch sw, SwitchInstance switchInstance) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        return populateSwitchInstance(br, sw, switchInstance);
    }

    public static SwitchInstance populateSwitchInstance(BufferedReader br, Switch sw, SwitchInstance switchInstance) throws IOException {
        String crt;
        int crtFlow = 0;
        while ((crt = br.readLine()) != null) {
            crt = crt.trim();
            if (crt.startsWith("table_add")) {
                String[] split = crt.split(" ");
                String tableName = split[1];
                String actionName = split[2];
                FlowInstance flowInstance = new FlowInstance().setTable(tableName).setFireAction(actionName);
                int j = 3;
                for (j = 3; j < split.length && !split[j].equals("=>"); j++) {
                    flowInstance.addMatchParams(split[j].trim());
                }
                j++;
                P4Action theActionTemplate = sw.getActionRegistrar().getAction(actionName);
                if (theActionTemplate == null)
                    throw new IllegalArgumentException("No such action " + actionName + " declared for switch");
                for (int k = 0; k < theActionTemplate.getParameterList().size(); k++, j++) {
                    if (IPAddressUtil.isIPv4LiteralAddress(split[j].trim())) {
                        flowInstance.addActionParams(RepresentationConversion.ipToNumber(split[j].trim()));
                    } else {
                        Pattern p = Pattern.compile("([0-9A-F]{2}[:-]){5}([0-9A-F]{2})");
                        if (p.matcher(split[j].trim().toUpperCase()).matches()) {
                            flowInstance.addActionParams(RepresentationConversion.macToNumber(split[j].trim().toUpperCase()));
                        } else {
                            try {
                                flowInstance.addActionParams(Long.decode(split[j].trim()));
                            } catch (NumberFormatException nfe) {
                                System.err.println("Match param failed at " + k + " line " + crt + " " + nfe.getMessage());
                                flowInstance.addActionParams(split[j].trim());
                            }
                        }
                    }
                }
                if (j < split.length) {
                    // last arg is always the priority
                    int prio = Integer.decode(split[j]);
                    flowInstance = flowInstance.setPriority(prio);
                } else {
                    List<TableMatch> matches = sw.getTableMatches(tableName);
                    int r = 0;
                    if (matches == null)
                        throw new IllegalArgumentException("No such table found " + tableName);
                    for (TableMatch tm : matches) {
                        if (tm.getMatchKind() == MatchKind.Lpm) {
                            String matchParm = flowInstance.getMatchParams().get(r).toString();
                            if (matchParm.contains("/")) {
                                try
                                {
                                    int mask = Integer.decode(matchParm.split("/")[1]);
                                    flowInstance.setPriority(-mask);
                                    break;
                                }
                                catch (NumberFormatException nfe) {
                                    System.err.println("Mask decode failed " + nfe.getMessage());
                                }
                            }
                            r++;
                        }
                    }
                }
                if (flowInstance.getPriority() == -1) {
                    flowInstance.setPriority(crtFlow);
                }
                switchInstance.add(flowInstance);
                crtFlow++;
            } else if (crt.startsWith("table_set_default")) {
                int j = 3;
                String[] split = crt.split(" ");
                String tableName = split[1];
                String actionName = split[2];
                P4ActionCall theCall = new P4ActionCall(switchInstance.getSwitchSpec().getActionRegistrar().getAction(actionName));
                for (; j < split.length; j++) {
                    if (IPAddressUtil.isIPv4LiteralAddress(split[j].trim())) {
                        theCall.addParameter(new P4ParameterInstance().setValue(RepresentationConversion.ipToNumber(split[j].trim()) + ""));
                    } else {
                        Pattern p = Pattern.compile("([0-9A-F]{2}[:-]){5}([0-9A-F]{2})");
                        if (p.matcher(split[j].trim().toUpperCase()).matches()) {
                            theCall.addParameter(new P4ParameterInstance().setValue(RepresentationConversion.macToNumber(split[j].trim()) + ""));
                        } else {
                            try {
                                theCall.addParameter(new P4ParameterInstance().setValue(Long.decode(split[j].trim()) + ""));
                            } catch (NumberFormatException nfe) {
                                theCall.addParameter(new P4ParameterInstance().setValue(split[j].trim()));
                            }
                        }
                    }
                }
                switchInstance.setDefaultAction(tableName, theCall);
            } else if (crt.startsWith("mirroring_add")) {
                String[] split = crt.split(" ");
                String clone = split[1];
                String egress = split[2];
                switchInstance.cloneSpec2EgressSpec.put(Integer.decode(clone), Integer.decode(egress));
            }
        }
        br.close();
        for (String table : switchInstance.getDeclaredTables()) {
            Collections.sort(switchInstance.flowInstanceIterator(table), (flowInstance, t1) -> flowInstance.getPriority() - t1.getPriority());
        }
        return switchInstance;
    }


}
