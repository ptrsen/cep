package com.csp.cep.collector;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esperio.csv.AdapterInputSource;
import com.espertech.esperio.csv.CSVInputAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Generator implements Runnable {

    private static final Logger log = LogManager.getLogger(Generator.class);

    private String  CsvEvent_File;                        //  CSV Events file to publish
    private boolean doStop = false;


    private  EPServiceProvider epService;
    private  String eventTypeName;

    public Generator(EPServiceProvider epService, String eventTypeName,String pathCsvEventsFile){
        this.epService = epService  ;
        this.eventTypeName = eventTypeName;
        this.CsvEvent_File = pathCsvEventsFile;
    }

    /**
     *  Publish events from csv file
     **/

    private void publishCsv() {
        log.debug("Publish events from Csv filename ...");
        AdapterInputSource source = new AdapterInputSource(CsvEvent_File);
        (new CSVInputAdapter(epService,source,eventTypeName)).start();
    }


    /**
     * Run Thread
     **/

    @Override
    public void run() {

        publishCsv();

        while(keepRunning()) {

        }

    }


    /**
     * Stops Thread
     **/

    public synchronized void doStop() {
        this.doStop = true;
    }


    /**
     * Checks thread is still running
     **/

    private synchronized boolean keepRunning() {
        return this.doStop == false;
    }


}

