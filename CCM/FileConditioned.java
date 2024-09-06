import star.common.*;
import java.io.*;
import java.util.*;
import star.base.neo.*;
import star.sixdof.*;
import star.base.report.*;
import star.motion.*;

public class FileConditioned extends StarMacro {

    private static final String FILE_PATH1 = "E:\\Casetest\\Star\\judge.txt";

    @Override
    public void execute() {
        Simulation simulation = getActiveSimulation();


        //Control
        for (int iter = 0; iter < 10000; iter++) { 
            int fileContent = readFileContent(FILE_PATH1); 

            if (fileContent == -1) {
                simulation.println("Error reading file. Please check the file path or permissions.");
                return;
            }

            if (fileContent != 0) {
                simulation.println("File content is non-zero. Pausing simulation.");
                simulation.getSimulationIterator().stop();

                boolean conditionMet = false;
                int retries = 0;
                while (!conditionMet && retries < 200000) {
                    try {
                        Thread.sleep(1000); 

                        int currentValue = readFileContent(FILE_PATH1);
                        if (currentValue == 0) {
                            conditionMet = true;
                            simulation.println("File content is zero. Resuming simulation.");
                            
                            try {
                                for (int i = 0; i < 29; i++) {
                                    simulation.getSimulationIterator().step(1);
                                }
                            } catch (Exception e) {
                                simulation.println("An error occurred: " + e.getMessage());
                                simulation.println("Forcing to continue to the next time step.");
                                simulation.getSimulationIterator().run();
                            }
                        } else {
                            simulation.println("File content is still non-zero. Waiting...");
                        }
                        retries++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                simulation.getSimulationIterator().step(1); //(29+1)

                //Control
                writeFileContent(FILE_PATH1, "1");


                //Force input
                String filePath = "E:\\Casetest\\Star\\bottomforce.txt";
                double[] forceComponents = new double[3];
                try {
                    BufferedReader br = new BufferedReader(new FileReader(filePath));
                    String line = br.readLine();
            
                    if (line != null) {
                        Scanner scanner = new Scanner(line);
                        for (int i = 0; i < 3; i++) {
                        if (scanner.hasNextDouble()) {
                            forceComponents[i] = scanner.nextDouble();
                            }
                        }
                        scanner.close();
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                double forceX = forceComponents[0];
                double forceZ = forceComponents[1];
                double forceY = forceComponents[2];

                ContinuumBody continuumBody = 
                    ((ContinuumBody) simulation.get(star.sixdof.BodyManager.class).getObject("MainBody"));//Body's Name
                ExternalForce externalForce = 
                    ((ExternalForce) continuumBody.getExternalForceAndMomentManager().getObject("CableForce"));//Force's Name
                    
                Units units_0 = ((Units) simulation.getUnitsManager().getObject("N"));
                externalForce.getForce().setComponentsAndUnits(forceX, forceY, forceZ, units_0);//Force value

                Units units_1 = ((Units) simulation.getUnitsManager().getObject("m"));
                externalForce.getPositionAsCoordinate().setCoordinate(units_1, units_1, units_1, new DoubleVector(new double[] {0.0, 0.0, 0.0}));//Force acting point

                //Foil control
                SixDofBodyMotion sixDofBodyMotion = ((SixDofBodyMotion) simulation.get(MotionManager.class).getObject("MainBody-Motion"));

                SuperposingRotatingMotion superposingRotatingMotion_1 = ((SuperposingRotatingMotion) sixDofBodyMotion.getSuperposingMotionManager().getObject("LeftFoil"));
                RotationRate rotationRate_2 = ((RotationRate) superposingRotatingMotion_1.getRotationSpecification());
                Units units_2 = ((Units) simulation.getUnitsManager().getObject("radian/s"));
                rotationRate_2.getRotationRate().setValueAndUnits(1.0, units_2);

                SuperposingRotatingMotion superposingRotatingMotion_2 = ((SuperposingRotatingMotion) sixDofBodyMotion.getSuperposingMotionManager().getObject("RightFoil"));
                RotationRate rotationRate_3 = ((RotationRate) superposingRotatingMotion_2.getRotationSpecification());
                Units units_3 = ((Units) simulation.getUnitsManager().getObject("radian/s"));
                rotationRate_3.getRotationRate().setValueAndUnits(1.0, units_3);



                //Write Velocity
                ReportMonitor reportMonitor_1 = ((ReportMonitor) simulation.getMonitorManager().getMonitor("Vx"));//Monitor's Name
                ReportMonitor reportMonitor_2 = ((ReportMonitor) simulation.getMonitorManager().getMonitor("Vy"));//Monitor's Name
                ReportMonitor reportMonitor_3 = ((ReportMonitor) simulation.getMonitorManager().getMonitor("Vz"));//Monitor's Name
                simulation.getMonitorManager().export("E:\\Casetest\\Star\\Velocity.csv", ",", new NeoObjectVector(new Object[] {reportMonitor_1, reportMonitor_2, reportMonitor_3}));//Output fileName
            }
        }
    }



    private int readFileContent(String filePath) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            if (line != null) {
                return Integer.parseInt(line.trim()); 
            } else {
                return -1; 
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


     private void writeFileContent(String filePath, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Simulation simulation = getActiveSimulation();
            simulation.println("Error writing to file: " + filePath);
        }
    }
}
