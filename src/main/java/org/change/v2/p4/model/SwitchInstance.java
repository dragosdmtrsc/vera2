package org.change.v2.p4.model;

import org.change.v2.p4.model.actions.P4Action;
import org.change.v2.p4.model.actions.P4ActionCall;
import org.change.v2.p4.model.actions.P4ActionProfile;
import org.change.v2.p4.model.actions.P4ParameterInstance;
import org.change.v2.p4.model.table.MatchKind;
import org.change.v2.p4.model.table.TableMatch;
import org.change.v2.util.conversion.RepresentationConversion;
import scala.Int;
import sun.net.util.IPAddressUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dragos on 31.08.2017.
 */
public class SwitchInstance implements ISwitchInstance {
    private Switch switchSpec;
    private String name;

    private Map<String, List<FlowInstance>> flowInstanceMap = new HashMap<String, List<FlowInstance>>();
    private Map<String, P4ActionCall> defaultActionMap = new HashMap<String, P4ActionCall>();

    private Map<Map.Entry<String, Integer>, String> actionProfileActions = new HashMap<>();
    private Map<Map.Entry<String, Integer>, List<Object>> actionProfileParams = new HashMap<>();

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
                if (theActionTemplate == null) {
                    Pattern pattern = Pattern.compile("member\\(([0-9]+)\\)");
                    Matcher matcher = pattern.matcher(actionName);
                    if (matcher.matches()) {
                        int member = Integer.decode(matcher.group(1));
                        P4ActionProfile prof = sw.getProfileByTable(tableName);
                        if (prof == null)
                            throw new IllegalArgumentException("No action profile declared for table " + tableName);
                        Map.Entry<String, Integer> theEntry =
                                new AbstractMap.SimpleEntry<String, Integer>(prof.getName(), member);
                        String actName = switchInstance.actionProfileActions.get(theEntry);
                        List<Object> actParams = switchInstance.actionProfileParams.get(theEntry);
                        for (Object parm : actParams)
                            flowInstance.setFireAction(actName).addActionParams(parm);
                        switchInstance.add(flowInstance);
                        continue;
                    } else {
                        Pattern grpPattern = Pattern.compile("group\\(([0-9]+)\\)");
                        Matcher grpMatcher = grpPattern.matcher(actionName);
                        if (grpMatcher.matches())
                            throw new UnsupportedOperationException("Groups not yet implemented");
                        else
                            throw new IllegalArgumentException("No such action " + actionName + " declared for switch");
                    }
                }
                j = parseActionArgs(crt, split, flowInstance, j, theActionTemplate);
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
                P4ActionCall theCall = null;
                if ("EMPTY".equals(actionName))
                    continue;
                if (!sw.getAllowedActions(tableName).contains(actionName)) {
                    Pattern pattern = Pattern.compile("member\\(([0-9]+)\\)");
                    Matcher matcher = pattern.matcher(actionName);
                    if (matcher.matches()) {
                        int member = Integer.decode(matcher.group(1));
                        P4ActionProfile prof = sw.getProfileByTable(tableName);
                        if (prof == null)
                            throw new IllegalArgumentException("No action profile declared for table " + tableName);
                        Map.Entry<String, Integer> theEntry =
                                new AbstractMap.SimpleEntry<String, Integer>(prof.getName(), member);
                        String actName = switchInstance.actionProfileActions.get(theEntry);
                        if (actName != null) {
                            List<Object> actParams = switchInstance.actionProfileParams.get(theEntry);
                            P4Action action = switchInstance.getSwitchSpec().getActionRegistrar().getAction(actName);
                            theCall = new P4ActionCall(action);
                            int r = 0;
                            if (actParams != null) {
                                for (Object parm : actParams)
                                    addParameter(parm.toString(), theCall).setParameter(action.getParameterList().get(r++));
                                switchInstance.setDefaultAction(tableName, theCall);
                            }
                        }


                        continue;
                    } else {
                        Pattern grpPattern = Pattern.compile("group\\(([0-9]+)\\)");
                        Matcher grpMatcher = grpPattern.matcher(actionName);
                        if (grpMatcher.matches())
                            throw new UnsupportedOperationException("Groups not yet implemented");
                        else
                            throw new IllegalArgumentException(tableName + " does not allow action " + actionName);
                    }
                } else {
                    P4Action action = switchInstance.getSwitchSpec().getActionRegistrar().getAction(actionName);
                    theCall = new P4ActionCall(action);
                    int r = 0;
                    for (; j < split.length; j++) {
                        addParameter(split[j], theCall).setParameter(action.getParameterList().get(r++));
                    }
                }
                switchInstance.setDefaultAction(tableName, theCall);
            } else if (crt.startsWith("mirroring_add")) {
                String[] split = crt.split(" ");
                String clone = split[1];
                String egress = split[2];
                switchInstance.cloneSpec2EgressSpec.put(Integer.decode(clone), Integer.decode(egress));
            } else if (crt.startsWith("act_prof_create_member")) {
                String[] split = crt.split(" ");
                String actProf = split[1];
                int nr = Integer.decode(split[2]);
                String actName = split[3];
                P4Action theActionTemplate = sw.getActionRegistrar().getAction(actName);
                List<Object> params = parseActionArgsToList(crt, split, 5, theActionTemplate);
                Map.Entry<String, Integer> tuple = new AbstractMap.SimpleEntry<String, Integer>(actProf, nr);
                switchInstance.actionProfileActions.put(tuple, actName);
                switchInstance.actionProfileParams.put(tuple, params);
            } else if (crt.startsWith("act_prof_create_group")) {
                throw new UnsupportedOperationException("Groups not yet implemented");
            }
        }
        br.close();
        for (String table : switchInstance.getDeclaredTables())
            Collections.sort(switchInstance.flowInstanceIterator(table), Comparator.comparingInt(FlowInstance::getPriority));
        return switchInstance;
    }

    private static P4ParameterInstance addParameter(String s, P4ActionCall theCall) {
        P4ParameterInstance p4ParameterInstance = new P4ParameterInstance();
        if (IPAddressUtil.isIPv4LiteralAddress(s.trim()))
            theCall.addParameter(p4ParameterInstance.setValue(RepresentationConversion.ipToNumber(s.trim()) + ""));
        else {
            Pattern p = Pattern.compile("([0-9A-F]{2}[:-]){5}([0-9A-F]{2})");
            if (p.matcher(s.trim().toUpperCase()).matches()) {
                theCall.addParameter(p4ParameterInstance.setValue(RepresentationConversion.macToNumber(s.trim()) + ""));
            } else {
                try {
                    theCall.addParameter(p4ParameterInstance.setValue(Long.decode(s.trim()) + ""));
                } catch (NumberFormatException nfe) {
                    theCall.addParameter(p4ParameterInstance.setValue(s.trim()));
                }
            }
        }
        return p4ParameterInstance;
    }

    private static List<Object> parseActionArgsToList(String crt, String []split,
                                                      int j,
                                                      P4Action theActionTemplate) {
        assert (theActionTemplate != null);
        if (theActionTemplate.getParameterList() == null)
            throw new IllegalArgumentException(theActionTemplate.getActionName() + " is wrong ");
        List<Object> args = new ArrayList<>();
        for (int k = 0; k < theActionTemplate.getParameterList().size(); k++, j++) {
            String s = split[j];
            if (IPAddressUtil.isIPv4LiteralAddress(s.trim())) {
                args.add(RepresentationConversion.ipToNumber(s.trim()));
            } else {
                Pattern p = Pattern.compile("([0-9A-F]{2}[:-]){5}([0-9A-F]{2})");
                if (p.matcher(s.trim().toUpperCase()).matches()) {
                    args.add(RepresentationConversion.macToNumber(s.trim().toUpperCase()));
                } else {
                    try {
                        args.add(Long.decode(s.trim()));
                    } catch (NumberFormatException nfe) {
                        System.err.println("Match param failed at " + k + " line " + crt + " " + nfe.getMessage());
                        args.add(s.trim());
                    }
                }
            }
        }
        return args;
    }

    private static int parseActionArgs(String crt, String []s, FlowInstance flowInstance, int j, P4Action theActionTemplate) {
        List<Object> params = parseActionArgsToList(crt, s, j, theActionTemplate);
        for (Object param : params) {
            flowInstance.addActionParams(param);
        }
        return j + params.size();
    }


}
