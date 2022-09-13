package banymixt;

import java.util.concurrent.Semaphore;

/**
 * Link video:https://youtu.be/7Ch-otlbNa4
 *
 * @author Joan Martorell Ferriol
 */
public class Treballador implements Runnable {

    public enum Sexe {
        HOME, DONA
    }

    //Constants
    private final static int HOMES = 6;
    private final static int DONES = 6;
    private final static int CAPACITAT = 3;
    private final static int ENTRADES_BANY = 2;
    private final static boolean DEBUG = true;

    private final static String[] NOMS_HOMES = {"GORI", "COSME", "JAUME", "DAMIA", "ANTONI", "BERNAT"};
    private final static String[] NOMS_DONES = {"AINA", "GERONIA", "CATALINA", "ELISABET", "JOANA", "FRANCESCA"};

    //Variables dels treballadors
    private String nom;
    private Sexe sexe;
    private boolean treballa;
    private int entrades; //Numero de veces que ha entrado en el baño

    //Variables compartides pels treballadors
    private static volatile int numHomes = 0;
    private static volatile int numDones = 0;

    //SEMAFORS
    public static Semaphore bany = new Semaphore(CAPACITAT, true);  //Controla la capacitat del bany
    public static Semaphore homes = new Semaphore(1);               //Controla l'acces a la variable numHomes
    public static Semaphore dones = new Semaphore(1);               //Controla l'acces a la variable numDones
    public static Semaphore coa = new Semaphore(1);                 //Bloquetja l'acces de treballadors de distint sexe

    //Contructor
    public Treballador(String nom, Sexe sexe) {
        this.nom = nom;
        this.entrades = 0;
        treballa = true;
        if (sexe == Sexe.DONA) {
            this.sexe = Sexe.DONA;
            System.out.println("\t" + this.nom + " arriba al despatx.");
        } else {
            this.sexe = Sexe.HOME;
            System.out.println(this.nom + " arriba al despatx.");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread[] homes = new Thread[HOMES];
        Thread[] dones = new Thread[DONES];

        //Llançament del processos
        for (int i = 0; i < HOMES; i++) {
            homes[i] = new Thread(new Treballador(NOMS_HOMES[i], Sexe.HOME));
            homes[i].start();
        }

        for (int i = 0; i < DONES; i++) {
            dones[i] = new Thread(new Treballador(NOMS_DONES[i], Sexe.DONA));
            dones[i].start();
        }

        //Espera a que acabin tots els processos
        for (int i = 0; i < HOMES; i++) {
            homes[i].join();
            System.out.println(NOMS_HOMES[i] + " ha acabat la feina");
        }

        for (int i = 0; i < DONES; i++) {
            dones[i].join();
            System.out.println("\t" + NOMS_DONES[i] + " ha acabat la feina");
        }
    }

    @Override
    public void run() {
        if (sexe == Sexe.HOME) {
            homes();
        } else {
            dones();
        }
    }

    /**
     * Dorm un proces durant una décima de segon
     */
    private void mySleep() {
        try {
            if (DEBUG) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            System.err.println(e.toString());
        }
    }

    public void homes() {
        while (entrades < ENTRADES_BANY) {

            if (treballa) {
                System.out.println(nom + " treballa");
                treballa = false;
            }
            mySleep();

            wait(coa);
            //Comprobam que no hi hagi dones
            if (numDones == 0) {
                //Comprobam si hi ha capacitat al bany
                wait(bany);
                //L'home entra al bany
                wait(homes);
                numHomes++;     //Incrementam el nombre d'homes que hi ha al bany
                signal(coa);
                
                entrades++;     //Incrementam el nombre d'entrades al bany
                System.out.println(nom + " entra " + entrades + "/" + ENTRADES_BANY + ". Homes al bany: " + numHomes + "\tDones al bany: " + numDones);
                signal(homes);
                mySleep();
                
                wait(homes);
                numHomes--;     //L'home surt del bany
                //Control de bany buit
                if (numHomes == 0 && numDones == 0) {
                    System.out.println(nom + " surt del bany\n" + "******** El bany està buit ********");
                } else {
                    System.out.println(nom + " surt del bany");
                }
                mySleep();
                signal(homes);

                signal(bany);
                treballa = true;
            } else {
                mySleep();
                signal(coa);
            }
        }
    }

    public void dones() {
        while (entrades < ENTRADES_BANY) {

            if (treballa) {
                System.out.println("\t" + nom + " treballa");
                treballa = false;
            }
            mySleep();
            wait(coa);
            //Comprobam que no hi hagi homes
            if (numHomes == 0) {
                //Comprobam si hi ha capacitat al bany
                wait(bany);
                //La dona entra al bany
                wait(dones);
                numDones++;     //Incrementam el nombre de dones que hi ha al bany
                signal(coa);
                
                entrades++;     //Incrementam el nombre d'entrades al bany
                System.out.println("\t" + nom + " entra " + entrades + "/" + ENTRADES_BANY + ". Dones al bany: " + numDones + "\tHomes al bany: " + numHomes);
                signal(dones);
                mySleep();
                
                wait(dones);
                numDones--;     //La dona surt del bany
                
                //Control de bany buit
                if (numHomes == 0 && numDones == 0) {
                    System.out.println("\t" + nom + " surt del bany\n" + "******** El bany està buit ********");
                } else {
                    System.out.println("\t" + nom + " surt del bany");
                }
                mySleep();
                signal(dones);

                signal(bany);
                treballa = true;
            } else {
                mySleep();
                signal(coa);
            }
        }
    }
    
    //MÉTODES DELS SEMAFORS
    private void wait(Semaphore s) {
        try {
            s.acquire();
        } catch (InterruptedException e) {
        }
    }

    private void signal(Semaphore s) {
        s.release();
    }
}
