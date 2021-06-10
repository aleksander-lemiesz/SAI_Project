package shared.model.broker;

public class StudentInfo {
    private int ec;
    private String name;

    public StudentInfo() {
    }

    public int getEc() {
        return ec;
    }

    public void setEc(int ec) {
        this.ec = ec;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "StudentInfo{" +
                "ec=" + ec +
                ", name='" + name + '\'' +
                '}';
    }
}
