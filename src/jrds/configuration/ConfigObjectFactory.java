package jrds.configuration;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Filter;
import jrds.GraphDesc;
import jrds.Macro;
import jrds.ProbeDesc;
import jrds.RdsHost;
import jrds.Tab;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsNode;
import jrds.probe.SumProbe;

import org.apache.log4j.Logger;

public class ConfigObjectFactory {
    static final private Logger logger = Logger.getLogger(ConfigObjectFactory.class);

    private ProbeFactory pf;
    private ClassLoader cl = this.getClass().getClassLoader();
    private Map<String, GraphDesc> graphDescMap = Collections.emptyMap();
    Map<String, Macro> macrosmap = Collections.emptyMap();
    private jrds.PropertiesManager pm = null;
    private Loader load = null;

    public ConfigObjectFactory(jrds.PropertiesManager pm){
        this.pm = pm;
        init();
    }

    public ConfigObjectFactory(jrds.PropertiesManager pm, ClassLoader cl){
        this.pm = pm;
        this.cl = cl;
        init();
    }	

    private void init() {
        try {
            load = new Loader();
            URL graphUrl = getClass().getResource("/desc");
            if(graphUrl != null)
                load.importUrl(graphUrl);
            else {
                logger.fatal("Default probes not found");
            }
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Loader initialisation error",e);
        }

        logger.debug(jrds.Util.delayedFormatString("Scanning %s for probes libraries", pm.libspath));
        for(URL lib: pm.libspath) {
            logger.info(jrds.Util.delayedFormatString("Adding lib %s", lib));
            load.importUrl(lib);
        }

        if(pm.configdir !=null)
            load.importDir(pm.configdir);
    }

    public void addUrl(URL ressourceUrl) {
        load.importUrl(ressourceUrl);
    }

    public Map<String, JrdsNode> getNodeMap(ConfigType ct) {
        return load.getRepository(ct);
    }

    public <BuildObject> Map<String, BuildObject> getObjectMap(ConfigObjectBuilder<BuildObject> ob, Map<String, JrdsNode> nodeMap) {
        Map<String, BuildObject> objectMap = new HashMap<String, BuildObject>();
        for(JrdsNode n: nodeMap.values()) {
            BuildObject o = null;
            String name;
            name = n.evaluate(ob.ct.getNameXpath());
            try {
                o = ob.build(n);
                if(o != null) {
                    objectMap.put(name, o);
                }
            } catch (InvocationTargetException e) {
                logger.error("Fatal error for object of type " + ob.ct + " and name " + name + ":" + e.getCause());
            }
        }
        return objectMap;
    }

    public Map<String, Macro> setMacroMap() {
        Map<String, JrdsNode> nodemap = load.getRepository(ConfigType.MACRODEF);
        macrosmap = getObjectMap(new MacroBuilder(), nodemap);
        logger.debug(jrds.Util.delayedFormatString("Macro configured: %s", macrosmap.keySet()));
        return macrosmap;
    }

    public Map<String, GraphDesc> setGrapMap() {
        Map<String, JrdsNode> nodemap = load.getRepository(ConfigType.GRAPH);
        Map<String, GraphDesc> graphsMap = getObjectMap(new GraphDescBuilder(), nodemap);
        logger.debug(jrds.Util.delayedFormatString("Graphs configured: %s", graphsMap.keySet()));
        return graphsMap;
    }

    public Map<String, GraphDesc> setGraphDescMap() {
        Map<String, JrdsNode> nodemap = load.getRepository(ConfigType.GRAPHDESC);
        GraphDescBuilder ob = new GraphDescBuilder();
        ob.setPm(pm);
        graphDescMap = getObjectMap(ob, nodemap);
        logger.debug(jrds.Util.delayedFormatString("Graph description configured: %s", graphDescMap.keySet()));
        return graphDescMap;
    }

    public Map<String, ProbeDesc> setProbeDescMap() {
        Map<String, JrdsNode> nodemap = load.getRepository(ConfigType.PROBEDESC);
        ProbeDescBuilder ob = new ProbeDescBuilder();
        ob.setClassLoader(cl);
        ob.setPm(pm);
        Map<String, ProbeDesc> probeDescMap = getObjectMap(ob, nodemap);
        pf = new ProbeFactory(probeDescMap, graphDescMap, pm);
        logger.debug(jrds.Util.delayedFormatString("Probe description configured: %s", probeDescMap.keySet()));
        return probeDescMap;
    }

    public Map<String, RdsHost> setHostMap() {
        Map<String, JrdsNode> nodemap = load.getRepository(ConfigType.HOSTS);
        HostBuilder ob = new HostBuilder();
        ob.setClassLoader(cl);
        ob.setMacros(macrosmap);
        ob.setProbeFactory(pf);
        ob.setPm(pm);
        Map<String, RdsHost> hostsMap = getObjectMap(ob, nodemap);
        logger.debug(jrds.Util.delayedFormatString("Hosts configured: %s", hostsMap.keySet()));
        return hostsMap;
    }

    public Map<String, Filter> setFilterMap() {
        Map<String, JrdsNode> nodemap = load.getRepository(ConfigType.FILTER);
        Map<String, Filter> filtersMap = getObjectMap(new FilterBuilder(), nodemap);
        logger.debug(jrds.Util.delayedFormatString("Filters configured: %s", filtersMap.keySet()));
        return filtersMap;
    }

    public Map<String, SumProbe> setSumMap() {
        SumBuilder ob =new SumBuilder();
        ob.setPm(pm);
        Map<String, JrdsNode> nodemap = load.getRepository(ConfigType.SUM);
        Map<String, SumProbe> sumpsMap = getObjectMap(ob, nodemap);
        logger.debug(jrds.Util.delayedFormatString("Sums configured: %s", sumpsMap.keySet()));
        return sumpsMap;
    }

    public Map<String, Tab> setTabMap() {
        Map<String, JrdsNode> nodemap = load.getRepository(ConfigType.TAB);
        Map<String, Tab> tabsMap = getObjectMap(new TabBuilder(), nodemap);
        logger.debug(jrds.Util.delayedFormatString("Tabs configured: %s", tabsMap.keySet()));
        return tabsMap;
    }

    public Set<Class<?>> getPreloadedClass() {
        return pf.getPreloadedClass();
    }
    /**
     * @return the load
     */
    Loader getLoader() {
        return load;
    }
    /**
     * @param load the load to set
     */
    void setLoader(Loader load) {
        this.load = load;
    }
}
