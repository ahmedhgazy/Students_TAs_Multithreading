import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class StudentThread extends SwingWorker<Void, String> {
    private int id;
    private Semaphore semaphoreTA;
    private Semaphore semaphoreChairs;
    private Lock mutex;
    private JLabel label;
    private JLabel laterLabel;

    private static int studentsComingLaterCount = 0;


    StudentThread(int id, Semaphore semaphoreTA, Semaphore semaphoreChairs, Lock mutex, JLabel label, JLabel laterLabel) {
        this.id = id;
        this.semaphoreTA = semaphoreTA;
        this.semaphoreChairs = semaphoreChairs;
        this.mutex = mutex;
        this.label = label;
        this.laterLabel = laterLabel;
    }

    @Override
    protected Void doInBackground() throws Exception {
        // Try to acquire a chair
        if (semaphoreChairs.tryAcquire()) {
            publish("Student " + id + " is waiting");
        } else {
            publish("Student " + id + " will come back later");
            laterLabel.setText("Students coming later: " + (++studentsComingLaterCount));
            return null;
        }

        // If successful, try to wake up the TA
        mutex.lock();
        semaphoreTA.acquire();
        mutex.unlock();

        // Get help from the TA
        publish("Student " + id + " is getting help");
        Thread.sleep(4000);

        // Release the TA
        semaphoreTA.release();

        // Release the chair
        semaphoreChairs.release();

        publish("Student " + id + " has finished");

        return null;
    }

    @Override
    protected void process(List<String> chunks) {
        String latestUpdate = chunks.get(chunks.size() - 1);
        label.setText(latestUpdate);
    }
}

class TAThread extends SwingWorker<Void, String> {
    private Semaphore semaphoreTA;
    private JLabel label;

    TAThread(Semaphore semaphoreTA, JLabel label) {
        this.semaphoreTA = semaphoreTA;
        this.label = label;
    }

    @Override
    protected Void doInBackground() throws Exception {
        while (true) {
            // Try to acquire the TA
            semaphoreTA.acquire();

            // Help the student
            publish("The TA is sleeping");
            Thread.sleep(2000);

            // Release the TA
            semaphoreTA.release();

            publish(" The TA is helping a student");
        }
    }

    @Override
    protected void process(List<String> chunks) {
        String latestUpdate = chunks.get(chunks.size() - 1);
        label.setText(latestUpdate);
    }
}

public class Main {
    public static void main(String[] args) {
        // Create the JFrame
        JFrame frame = new JFrame("Sleeping Teaching Assistant");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new FlowLayout());

        // Create the semaphores
        Semaphore semaphoreTA = new Semaphore(1);
        Semaphore semaphoreChairs = new Semaphore(3);

        // Create the mutex lock
        Lock mutex = new ReentrantLock();

        // Create a label for the TA
        JLabel labelTA = new JLabel("The TA is sleeping");
        frame.add(labelTA);

        // Create a button to create a TA
        JButton buttonTA = new JButton("Create TA");
        buttonTA.addActionListener(e -> {
            TAThread ta = new TAThread(semaphoreTA, labelTA);
            ta.execute();
        });
        frame.add(buttonTA);

        // Create a panel for the students
        JPanel panelStudents = new JPanel();
        panelStudents.setLayout(new BoxLayout(panelStudents, BoxLayout.Y_AXIS));
        frame.add(new JScrollPane(panelStudents));

        // A label for students coming later
        JLabel laterLabel = new JLabel("Students coming later: 0");
        frame.add(laterLabel);

        // Create a button to create a student
        JButton buttonStudent = new JButton("Create Student");
        buttonStudent.addActionListener(e -> {
            JLabel labelStudent = new JLabel("No students yet");
            panelStudents.add(labelStudent);
            StudentThread student = new StudentThread(panelStudents.getComponentCount(), semaphoreTA, semaphoreChairs, mutex, labelStudent, laterLabel);
            student.execute();
            frame.validate();
        });
        frame.add(buttonStudent);

        // Show the JFrame
        frame.setVisible(true);
    }
}