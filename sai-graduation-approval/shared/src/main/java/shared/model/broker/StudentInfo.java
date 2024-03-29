package shared.model.broker;

/**
 * Class representing student details that are received from the student administration system.
 */
public class StudentInfo {
    private int graduationPhaseECs;
    private String mentor;

    public StudentInfo() {
    }

    public int getGraduationPhaseECs() {
        return graduationPhaseECs;
    }

    public void setGraduationPhaseECs(int graduationPhaseECs) {
        this.graduationPhaseECs = graduationPhaseECs;
    }

    public String getMentor() {
        return mentor;
    }

    public void setMentor(String mentor) {
        this.mentor = mentor;
    }

    @Override
    public String toString() {
        return "StudentInfo{" +
                "ec=" + graduationPhaseECs +
                ", name='" + mentor + '\'' +
                '}';
    }
}
