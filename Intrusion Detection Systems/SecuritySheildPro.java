import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

class IDS {
     static int days;
     static String eventFileName;
     static String statsFileName;
     static ArrayList<Event> eventList = new ArrayList<Event>();
     static ArrayList<Stats> statList = new ArrayList<Stats>();
     static ArrayList<Stats> newStatsList = new ArrayList<Stats>();
     static HashMap<String, ArrayList<Double>> individualEventValue;
     static HashMap<String, Double> individualEventTotal;
     static HashMap<String, Double> individualEventMean;
     static HashMap<String, Double> individualEventStandardDeviation;
     static HashMap<Integer, Double> dailyTotal;
     static HashMap<String, List<Double>> baseLineResultsMap;
     static HashMap<String, List<Double>> liveResultsMap;
     static int threshold;
     static boolean readEventFile(String fileName) {

        String[] temp;
        String eventName, eventType;
        double minimum, maximum;
        int weight;
        boolean isDiscrete;
        int line = 0, events = 0;

        try {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            System.out.println("----------------------------------------------------------------------");
            System.out.println("Processing " + fileName);
            System.out.println("----------------------------------------------------------------------");
            while (scanner.hasNextLine()) {
                if (line == 0) {
                    events = scanner.nextInt();
                    scanner.nextLine();
                    line++;
                } else {

                    temp = scanner.nextLine().split(":");
                    eventName = temp[0];
                    eventType = temp[1];

                    isDiscrete = eventType.charAt(0) == 'D';
                    if (isDiscrete) {
                        minimum = Integer.parseInt(temp[2]);
                        if (temp[3].equals("")) {
                            maximum = 999999d;
                        } else {
                            maximum = Integer.parseInt(temp[3]);
                        }
                    } else {
                        minimum = Double.parseDouble(temp[2]);

                        if (temp[3].equals("")) {
                            maximum = 999999f;
                        } else {
                            maximum = Double.parseDouble(temp[3]);
                        }
                    }

                    if (minimum > maximum) {
                        System.out .println("Error. Event " + eventName + ", minimum value is larger than maximum value.");
                        return false;
                    }
                    weight = Integer.parseInt(temp[4]);
                    threshold += weight;
                    if (weight < 0) {
                        System.out.println("Error. Event " + eventName + ", weight is not a positive value.");
                        return false;
                    }
                    eventList.add(new Event(eventName, isDiscrete, minimum, maximum, weight));
                    line++;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        line--;

        if (events != line && line > 0) {
            System.out.println("Error. Number of events specified does not tally with actual number of event records.");
        }

        System.out.println(line + " lines successfully read in!");

        return true;

    }

     static boolean readStatsFile(String fileName, List<Stats> statList) {
        String[] temp;
        String eventName;
        double mean, standardDeviation;

        int line = 0, events = 0;

        try {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);

            System.out.println("\n----------------------------------------------------------------------");
            System.out.println("Processing " + fileName);
            System.out.println("----------------------------------------------------------------------");

            while (scanner.hasNextLine()) {
                if (line == 0) {
                    events = scanner.nextInt();
                    scanner.nextLine();
                    line++;
                } else {
                    temp = scanner.nextLine().split(":");
                    eventName = temp[0];

                    if (temp[1].equals("")) {
                        mean = 999999f;
                    } else {
                        mean = Double.parseDouble(temp[1]);
                    }
                    if (temp[2].equals("")) {
                        standardDeviation = 999999f;
                    } else {
                        standardDeviation = Double.parseDouble(temp[2]);
                    }
                    statList.add(new Stats(eventName, mean, standardDeviation));
                    line++;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        line--;
        if (events != line && line > 0) {
            System.out.println("Error. Number of stats specified does not tally with actual number of stats records.");
        }

        System.out.println(line + " lines successfully read in!");
        return false;
    }

    

     static void displayData() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Displaying data");
        System.out.println("----------------------------------------------------------------------");

        for (Event e : eventList) {
            System.out.println(e);
        }

        System.out.println();
        for (Stats s : statList) {
            System.out.println(s);
        }
    }

    public static void readAndDisplayFiles() {
        if (!readEventFile(eventFileName) || readStatsFile(statsFileName, statList)) {
            System.out.println("Error. Detected errors and inconsistencies in the file.");
            System.exit(0);
        }

        displayData();
    }

     static void discreteEvent(String name, int minimum, int maximum, double mean,
            double standardDeviation, boolean isAlert, int day, int serverSecret, FileWriter fw) {
        Random rand = new Random();
        int tempMin = 0;
        try {
            if (isAlert) {
                if (rand.nextInt(3) == 1) {
                    tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean) * serverSecret * 3;
                } else {
                    tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean);
                }
            } else {
                tempMin = (int) Math.round(rand.nextGaussian() * standardDeviation + mean);
            }
            tempMin = Math.abs(tempMin);
            fw.write(name + ": " + tempMin + "\n");

            if (individualEventValue.containsKey(name)) {
                individualEventValue.get(name).add((double) tempMin);
            } else {
                ArrayList<Double> tempList = new ArrayList<>();
                tempList.add((double) tempMin);
                individualEventValue.put(name, tempList);
            }

            if (dailyTotal.containsKey(day)) {
                dailyTotal.put(day, dailyTotal.get(day) + tempMin);
            } else {
                dailyTotal.put(day, (double) tempMin);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void continuousEvent(String name, double minimum, double maximum, double mean,
            double standardDeviation, boolean isAlert, int day, int serverSecret, FileWriter fw) {
        Random rand = new Random();
        double tempMin = 0;
        try {
            if (isAlert) {
                if (rand.nextInt(3) == 1) {
                    tempMin = (Math.round((rand.nextGaussian() * standardDeviation + mean) * 100.0) / 100.0)* serverSecret * 3;
                } else {
                    tempMin = Math.round((rand.nextGaussian() * standardDeviation + mean) * 100.0) / 100.0;
                }
            } else {
                tempMin = Math.round((rand.nextGaussian() * standardDeviation + mean) * 100.0) / 100.0;
            }

            tempMin = Math.abs(tempMin);
            fw.write(name + ": " + tempMin + "\n");

            if (individualEventValue.containsKey(name)) {
                individualEventValue.get(name).add(tempMin);
            } else {
                ArrayList<Double> tempList = new ArrayList<>();
                tempList.add(tempMin);
                individualEventValue.put(name, tempList);
            }

            if (dailyTotal.containsKey(day)) {
                dailyTotal.put(day, dailyTotal.get(day) + tempMin);
            } else {
                dailyTotal.put(day, tempMin);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void formulate() {
        for (String s : individualEventValue.keySet()) {

            double sum = 0;
            for (double d : individualEventValue.get(s))
                sum += d;

            individualEventTotal.put(s, sum);
        }

        for (String s : individualEventTotal.keySet()) {

            double totalTemp = individualEventTotal.get(s);
            double tempMean = Math.round((totalTemp) / days * 100) / 100.0;
            individualEventMean.put(s, tempMean);

            double tempSD = 0;
            for (double d : individualEventValue.get(s))
                tempSD += Math.pow(d - tempMean, 2);

            tempSD = Math.round(Math.sqrt(tempSD / days) * 100) / 100.0;
            individualEventStandardDeviation.put(s, tempSD);
        }
    }
    public static void activity(boolean isAlert, List<Stats> statList) {

        individualEventValue = new HashMap<>();
        individualEventTotal = new HashMap<>();
        individualEventMean = new HashMap<>();
        individualEventStandardDeviation = new HashMap<>();
        dailyTotal = new HashMap<>();

        Random rand = new Random();

        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Activity Engine running");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Generating events...");

        String name;
        int serverSecret;
        double mean, standardDeviation;

        try {
            LocalTime lt = LocalTime.now();
            LocalDate ld = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HHmmss");
            DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("ddMMyyyy");
            String fileName = dtf2.format(ld) + "-" + dtf.format(lt) + "-activity.txt";

            FileWriter fw = new FileWriter(fileName);

            for (int i = 1; i <= days; i++) {
                fw.write("Day " + i + " :" + "\n");
                for (Event e : eventList) {
                    for (Stats s : statList) {
                        if (e.getEventName().equals(s.getEventName())) {
                            serverSecret = rand.nextInt() % days;
                            name = e.getEventName();
                            mean = s.getMean();
                            standardDeviation = s.getStandardDeviation();
                            if (e.isDiscrete()) {
                                discreteEvent(name, e.getMin(), e.getMax(), mean, standardDeviation,isAlert, i, serverSecret, fw);
                            } 
                            else {
                                continuousEvent(name, e.getMinimum(), e.getMaximum(), mean, standardDeviation,isAlert, i, serverSecret, fw);
                            }
                        }
                    }
                }
                fw.write("\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        formulate();
    }

    public static void analysis(HashMap<String, List<Double>> resultsMap) {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Analysis Engine running");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Analysing generated events data...");

        try {
            LocalTime lt = LocalTime.now();
            LocalDate ld = LocalDate.now();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HHmmss");
            DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("ddMMyyyy");
            String fileName = dtf2.format(ld) + "-" + dtf.format(lt) + "-analysis.txt";

            FileWriter fw = new FileWriter(fileName);

            fw.write("--------------Statistical Data--------------\n");
            for (String s : individualEventTotal.keySet()) {
                fw.write(s + "\n");
                fw.write("Total: " + individualEventTotal.get(s) + "\n");
                fw.write("Mean: " + individualEventMean.get(s) + "\n");
                fw.write("SD: " + individualEventStandardDeviation.get(s) + "\n");
                fw.write("\n");

                ArrayList<Double> tempList = new ArrayList<>();
                tempList.add(individualEventTotal.get(s));
                tempList.add(individualEventMean.get(s));
                tempList.add(individualEventStandardDeviation.get(s));
                resultsMap.put(s, tempList);

            }
            fw.write("--------------Daily Totals--------------\n");
            for (int i : dailyTotal.keySet()) {
                fw.write("Day " + i + ":\t" + dailyTotal.get(i) + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void newStats() {
        String newStatsFileName = null;
        Scanner input = new Scanner(System.in);
        System.out.println("\nPlease enter new statistics filename ");
        newStatsFileName = input.nextLine();
        // newStatsFileName = "newStats.txt";
        if (readStatsFile(newStatsFileName, newStatsList)) {
            System.out.println("Error. Detected errors and inconsistencies in the file.");
            System.exit(0);
        }
        System.out.println();
        for (Stats s : newStatsList) {
            System.out.println(s);
        }
        do {
            System.out.println("\nPlease enter number of days: ");
            days = input.nextInt();
        } while (days <= 0);
    }
    public static void alert() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("Alert Engine running");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Discovering Anomalies...");
        double mean, sd, actualValue;
        int weight = 0;
        double baseMean, baseSD;
        for (int i = 0; i < days; i++) {
            double anomalyCounter = 0, anomaly = 0;
            System.out.println("\n----------------------Day " + (i + 1) + "----------------------------");
            for (String s : individualEventValue.keySet()) {

                actualValue = Math.abs(individualEventValue.get(s).get(i));
                if (liveResultsMap.containsKey(s)) {
                    mean = liveResultsMap.get(s).get(1);
                    sd = liveResultsMap.get(s).get(2);
                    for (Event e : eventList) {
                        if (e.getEventName().equals(s))
                            weight = e.getWeight();
                    }
                    if (actualValue < mean) {
                        anomaly = ((mean - actualValue) / sd) * weight;
                    } else {
                        anomaly = ((actualValue - mean) / sd) * weight;
                    }
                    if (baseLineResultsMap.containsKey(s)) {
                        baseMean = baseLineResultsMap.get(s).get(1);
                        baseSD = baseLineResultsMap.get(s).get(2);
                        if (actualValue > (baseMean + baseSD) || actualValue < (baseMean - baseSD)) {
                            anomaly *= 3;
                        }
                    }
                    anomalyCounter += anomaly;
                }

                System.out.printf("Event: %-20s %s: %-15.2f %s: %.2f\n", s, "Actual Num", actualValue, "Anomaly",anomaly);

            }
            System.out.printf("\nDaily Counter: %.6f\n", anomalyCounter);
            System.out.printf("Threshold: %d\n", threshold);
            System.out.println(anomalyCounter > threshold ? "<<<ALERT>>>" : "No Alert");
        }
    }

    public static void run() {
        baseLineResultsMap = new HashMap<>();
        liveResultsMap = new HashMap<>();
        readAndDisplayFiles();
        activity(false, statList);
        analysis(baseLineResultsMap);
        // threshold *= 2;
        newStats();
        activity(true, newStatsList);
        analysis(liveResultsMap);
        alert(); 
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter filename for event data: ");
        eventFileName = scanner.nextLine();
        // eventFileName = "Events.txt";

        System.out.println("Enter filename for stats data: ");
        statsFileName = scanner.nextLine();
        // statsFileName ="Stats.txt";

        int days = 0;
        while (days <= 0) {
            System.out.println("Enter the number of days for simulation: ");
            try {
                days = scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a positive integer for days.");
            }
        }
        run();
    }

}
