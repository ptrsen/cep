package com.csp.cep.processor;
import com.csp.cep.annotation.*;

import com.espertech.esper.client.*;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.deploy.Module;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.lang.annotation.Annotation;



public class Engine implements Runnable {

    private static final Logger log = LogManager.getLogger(Engine.class);

    private String  Configuration_File;         //  Configuration EPL file  (contains Schema and Rules)
    private boolean doStop = false;             //  Stops thread

    private  EPServiceProvider epService;
    private  EPDeploymentAdmin deployAdmin;
    private  EPRuntime runtime;
    private  String eventTypeName;


    /**
     * Setup Esper Engine with default values
     **/

    public Engine(){
        log.debug("CEP Engine Setup ...");
        this.epService = EPServiceProviderManager.getDefaultProvider();
        this.deployAdmin = epService.getEPAdministrator().getDeploymentAdmin();
    }


    /**
     *     Starts Esper Engine
     **/

    @Override
    public void run() {

        this.runtime = epService.getEPRuntime();
        log.debug("Successfully started Esper Runtime ...");

        while(keepRunning()) {
/*
            System.out.println("Running thread");


           //  elapsed time
            try {
                Thread.sleep(1L * 500L); // 0.5 seg
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            */
        }


   }


    /**
     * Register new Event Schema and Statements (with attached Subscriptor or Listener) in EPL file
     **/

    public void updateRule(String pathConfFile) throws Exception {
       this.Configuration_File = pathConfFile;

       log.debug("Deleting previous registered Event Schema and Statements  ...");
       epService.getEPAdministrator().destroyAllStatements();

       log.debug("Reading EPL file ...");
       Module queries = deployAdmin.read(Configuration_File);
       String moduleDeploymentId = deployAdmin.add(queries);
       deployAdmin.deploy(moduleDeploymentId, null);

       // Registering and adding subscriptor
       log.debug("Registering new Event Schema and Statements  ...");
       String [] StatementsN = epService.getEPAdministrator().getStatementNames();


       String [] text = epService.getEPAdministrator().getStatement("CreateEventSchema").getText().split("\n");

       eventTypeName = text[2].split(" ")[2];


        // Search for Rule statements and add a subscriptor ( same one for everyone)
       for (String Names : StatementsN){
           if(Names.contains("Rule_")){  // Rule_Name -> format (just register queries with the format Rule_...)
               processAnnotations(epService.getEPAdministrator().getStatement(Names)); // process statements and attach a subscriptor or listener
           }
       }

   }

    /**
     * Prcess Annotations in EPL file , Attach Subscriptor or Listener to Rules
     **/

    private static void processAnnotations(EPStatement statement) throws Exception {

        Annotation[] annotations = statement.getAnnotations();
        for (Annotation annotation : annotations) {

            // For Esper Subscriber
            if (annotation instanceof Subscriber){
                Subscriber subscriber  = (Subscriber) annotation;
                Class<?> cl = Class.forName(subscriber.value());
                Object obj = cl.newInstance();
                statement.setSubscriber(obj);
            }

            // For Esper Listeners StatementAwareUpdateListener or UpdateListener
            if (annotation instanceof Listeners) {
                Listeners listeners = (Listeners) annotation;
                for (String values : listeners.value()) {
                    Class<?> cl = Class.forName(values);
                    Object obj = cl.newInstance();
                    if (obj instanceof StatementAwareUpdateListener) {
                        statement.addListener((StatementAwareUpdateListener) obj);

                    } else {
                        statement.addListener((UpdateListener) obj);
                    }
                }
            }

        }
    }



    /**
     * Return EPserviceProvider
     **/

   public EPServiceProvider getEpservice (){
        return epService;
   }

    /**
     * Return EventTypeName from Schema
     **/

    public String getEventTypeName (){
        return eventTypeName;
    }


    /**
     *   Destroy Engine
     **/

    public void destroy(){
       epService.destroy();
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
