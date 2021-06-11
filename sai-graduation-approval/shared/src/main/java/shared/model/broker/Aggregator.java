package shared.model.broker;

import shared.model.approval.ApprovalReply;
import shared.model.approval.ApprovalRequest;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Class storing the additional information about the ApprovalRequest
 */
public class Aggregator {

    // Unique ID used to distinguish the Aggregators
    private int aggregationID = 0;
    // Number of replies that have to be delivered in order to make final reply
    private int numberOfRepliesExpected = 0;
    // The request itself
    private ApprovalRequest request = null;
    // Number of replies already received
    private int numberOfReplies = 0;
    // List of replies to the request
    private ArrayList<ApprovalReply> replies = new ArrayList<>();

    /**
     * Default constructor.
     */
    public Aggregator() {
    }

    /**
     * Parametrized constructor.
     * @param aggregationID the Aggregation ID of new Aggregator.
     * @param request the request to be stored inside the aggregator.
     */
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

    /**
     * Function to add a new reply.
     * @param reply is the reply sent from the approval.
     */
    public void AddReply(ApprovalReply reply) {
        // If there are no replies just add a new one and increase the number of replies
        if (replies.isEmpty()) {
            replies.add(reply);
            numberOfReplies = 1;
        } else {
            // Check if the aggregator is not ready for final reply
            if (!isReadyForFinalReply()) {
                // Check if that reply was not already added
                for (ApprovalReply r : replies) {
                    if (!r.getName().equals(reply.getName())) {
                        // Add a new reply and increase the number of replies and finish the loop
                        replies.add(reply);
                        numberOfReplies++;
                        return;
                    } else {
                        // If the same approval sends another reply just change the boolean value to the new one
                        r.setApproved(reply.isApproved());
                    }
                }
            }
        }
    }

    /**
     * Checks if the request received enough replies.
     * @return is true if all approvals that were supposed to replied, if not it is false.
     */
    public boolean isReadyForFinalReply() {
        return numberOfReplies == numberOfRepliesExpected;
    }

    /**
     * Evaluates if the request is approved or not basing on the replies.
     * @return is true if all of the replies approve the request, if not it is false.
     */
    public ApprovalReply getEvaluatedApprovalReply() {
        if (replies.isEmpty()) {
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
