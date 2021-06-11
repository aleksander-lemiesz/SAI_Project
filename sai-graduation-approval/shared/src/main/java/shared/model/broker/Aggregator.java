package shared.model.broker;

import shared.model.approval.ApprovalReply;
import shared.model.approval.ApprovalRequest;

import java.util.ArrayList;
import java.util.Objects;

public class Aggregator {

    private int aggregationID = 0;
    private int numberOfRepliesExpected = 0;
    private ApprovalRequest request = null;
    private int numberOfReplies = 0;
    private ArrayList<ApprovalReply> replies = new ArrayList<>();

    public Aggregator() {
    }

    public Aggregator(int aggregationID, ApprovalRequest request) {
        this.aggregationID = aggregationID;
        this.request = request;
    }

    public int getAggregationID() {
        return aggregationID;
    }

    public void setAggregationID(int aggregationID) {
        this.aggregationID = aggregationID;
    }

    public int getNumberOfRepliesExpected() {
        return numberOfRepliesExpected;
    }

    public void setNumberOfRepliesExpected(int numberOfRepliesExpected) {
        this.numberOfRepliesExpected = numberOfRepliesExpected;
    }

    public ApprovalRequest getRequest() {
        return request;
    }

    public void setRequest(ApprovalRequest request) {
        this.request = request;
    }

    public int getNumberOfReplies() {
        return numberOfReplies;
    }

    public ArrayList<ApprovalReply> getReplies() {
        return new ArrayList<>(replies);
    }

    public void AddReply(ApprovalReply reply) {
        if (replies.isEmpty()) {
            System.out.println("Reply to be added: " + reply);
            replies.add(reply);
            System.out.println("After adding the reply: " + this);
            numberOfReplies = 1;
        } else {
            if (!isReadyForFinalReply()) {
                for (ApprovalReply r : replies) {
                    if (!r.getName().equals(reply.getName())) {
                        replies.add(reply);
                        numberOfReplies++;
                        return;
                    } else {
                        r.setApproved(reply.isApproved());
                    }
                }
            }
        }
    }

    public boolean isReadyForFinalReply() {
        return numberOfReplies == numberOfRepliesExpected;
    }

    public ApprovalReply getEvaluatedApprovalReply() {
        if (replies.isEmpty()) {
            System.out.println("The list is empty!");
            return null;
        } else {
            ApprovalReply evaluatedReply = new ApprovalReply();
            evaluatedReply.setApproved(replies.get(0).isApproved());
            evaluatedReply.setName(replies.get(0).getName());

            for (ApprovalReply reply : replies) {
                if (!reply.isApproved()) {
                    evaluatedReply.setName(reply.getName());
                    evaluatedReply.setApproved(reply.isApproved());
                }
            }
            return evaluatedReply;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Aggregator that = (Aggregator) o;
        return aggregationID == that.aggregationID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(aggregationID);
    }

    @Override
    public String toString() {
        return "Aggregator{" +
                "aggregationID=" + aggregationID +
                ", numberOfRepliesExpected=" + numberOfRepliesExpected +
                ", request=" + request +
                ", numberOfReplies=" + numberOfReplies +
                ", replies=" + replies +
                '}';
    }
}
