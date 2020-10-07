package pa2;

import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator {
  /*
   * Predefined Constants (static member variables):
   *
   * int MAXDATASIZE : the maximum size of the Message data and Packet payload
   *
   * int A : a predefined integer that represents entity A int B : a predefined integer that
   * represents entity B
   *
   * Predefined Member Methods:
   *
   * void stopTimer(int entity): Stops the timer running at "entity" [A or B] void startTimer(int
   * entity, double increment): Starts a timer running at "entity" [A or B], which will expire in
   * "increment" time units, causing the interrupt handler to be called. You should only call this
   * with A. void toLayer3(int callingEntity, Packet p) Puts the packet "p" into the network from
   * "callingEntity" [A or B] void toLayer5(String dataSent) Passes "dataSent" up to layer 5 double
   * getTime() Returns the current time in the simulator. Might be useful for debugging. int
   * getTraceLevel() Returns TraceLevel void printEventList() Prints the current event list to
   * stdout. Might be useful for debugging, but probably not.
   *
   *
   * Predefined Classes:
   *
   * Message: Used to encapsulate a message coming from layer 5 Constructor: Message(String
   * inputData): creates a new Message containing "inputData" Methods: boolean setData(String
   * inputData): sets an existing Message's data to "inputData" returns true on success, false
   * otherwise String getData(): returns the data contained in the message Packet: Used to
   * encapsulate a packet Constructors: Packet (Packet p): creates a new Packet that is a copy of
   * "p" Packet (int seq, int ack, int check, String newPayload) creates a new Packet with a
   * sequence field of "seq", an ack field of "ack", a checksum field of "check", and a payload of
   * "newPayload" Packet (int seq, int ack, int check) chreate a new Packet with a sequence field of
   * "seq", an ack field of "ack", a checksum field of "check", and an empty payload Methods:
   * boolean setSeqnum(int n) sets the Packet's sequence field to "n" returns true on success, false
   * otherwise boolean setAcknum(int n) sets the Packet's ack field to "n" returns true on success,
   * false otherwise boolean setChecksum(int n) sets the Packet's checksum to "n" returns true on
   * success, false otherwise boolean setPayload(String newPayload) sets the Packet's payload to
   * "newPayload" returns true on success, false otherwise int getSeqnum() returns the contents of
   * the Packet's sequence field int getAcknum() returns the contents of the Packet's ack field int
   * getChecksum() returns the checksum of the Packet int getPayload() returns the Packet's payload
   *
   */

  /*
   * Please use the following variables in your routines. int WindowSize : the window size double
   * RxmtInterval : the retransmission timeout int LimitSeqNo : when sequence number reaches this
   * value, it wraps around
   */

  public static final int FirstSeqNo = 0;
  private int WindowSize;
  private double RxmtInterval;
  private int LimitSeqNo;

  // Add any necessary class variables here. Remember, you cannot use
  // these variables to send messages error free! They can only hold
  // state information for A or B.
  // Also add any necessary methods (e.g. checksum of a String)

  // This is the constructor. Don't touch!
  public StudentNetworkSimulator(int numMessages, double loss, double corrupt, double avgDelay,
      int trace, int seed, int winsize, double delay) {
    super(numMessages, loss, corrupt, avgDelay, trace, seed);
    WindowSize = winsize;
    LimitSeqNo = winsize * 2; // set appropriately; assumes SR here!
    RxmtInterval = delay;
  }

  private int transmittedNum = 0;
  private int retransmissionsNum = 0;
  private int ACKNum = 0;
  private int packetLossNum = 0;
  private int corruptedPacketNum = 0;
  private double totalRTT = 0;
  private double startTime = 0;
  private int RTT = 0;


  private int aSeqNum;
  private Packet aPacket;

  private int bSeqNum;
  private Packet bPacket;

  private boolean inTransit;
  private boolean ackIgnored;

  private int TIME = 130;

  // translate String 'str' into an int, and add this to seq and ack to generate a checksum
  // then return that checksum



  // This routine will be called whenever the upper layer at the sender [A]
  // has a message to send. It is the job of your protocol to insure that
  // the data in such a message is delivered in-order, and correctly, to
  // the receiving upper layer.
  protected void aOutput(Message message) {
    if (!inTransit) {
      aPacket = makePacket(aSeqNum, A, message.getData());
      send(aPacket);
      inTransit = true;
      startTime = getTime();
      transmittedNum++;
    } else {
      System.out.println("A: message - " + message.getData() + "was dropped");
    }
  }

  // This routine will be called whenever a packet sent from the B-side
  // (i.e. as a result of a toLayer3() being done by a B-side procedure)
  // arrives at the A-side. "packet" is the (possibly corrupted) packet
  // sent from the B-side.
  protected void aInput(Packet packet) {
    updateRTT();

    System.out.println("A: rcv ACK" + packet.getAcknum());
    Packet newPacket = makePacketForChecksum(packet);

    RTT++;
    // Check to make sure that the Acknowledgement is for the correct packet
    if (!corrupt(newPacket) && packet.getAcknum() == aSeqNum) {
      System.out.println("A: AlternatingBit.Packet" + packet.getAcknum() + " was acknowledged");
      System.out.println("A: Stopped timer");
      aSeqNum = computeSeqNum(aSeqNum);
      inTransit = false;
      stopTimer(A);

    } else if (corrupt(newPacket)) {
      System.out.println("A: received a corrupted ACK");
      corruptedPacketNum++;
    } else {
      System.out.println("A: received wrong ACK");
      ackIgnored = true;
    }
  }

  private void updateRTT() {
    totalRTT += getTime() - startTime;
    RTT++;
  }

  // This routine will be called when A's timer expires (thus generating a
  // timer interrupt). You'll probably want to use this routine to control
  // the retransmission of packets. See startTimer() and stopTimer(), above,
  // for how the timer is started and stopped.
  protected void aTimerInterrupt() {
    if (!ackIgnored) {
      packetLossNum++;
      ackIgnored = false;
    }

    System.out.println("A: Timed out........" + aPacket.getPayload());
    send(aPacket);
    startTime = getTime();
    retransmissionsNum++;

  }

  // This routine will be called once, before any of your other A-side
  // routines are called. It can be used to do any required
  // initialization (e.g. of member variables you add to control the state
  // of entity A).
  protected void aInit() {
    aSeqNum = 0;
    inTransit = false;
    ackIgnored = false;
  }

  // This routine will be called whenever a packet sent from the B-side
  // (i.e. as a result of a toLayer3() being done by an A-side procedure)
  // arrives at the B-side. "packet" is the (possibly corrupted) packet
  // sent from the A-side.
  protected void bInput(Packet packet) {
    System.out.println("B: rcv pkt" + packet.getSeqnum() + " data: " + packet.getPayload());
    if (!corrupt(packet) && packet.getSeqnum() == bSeqNum) {

      bPacket = packet;

      toLayer5(packet.getPayload());

      ACKNum++;

      bSeqNum = computeSeqNum(bSeqNum);
    } else if (corrupt(packet) || packet.getSeqnum() != bSeqNum) {

      if (corrupt(packet)) {
        System.out.println("B: Detected a corrupt packet from A");
        corruptedPacketNum++;
      }

      if (packet.getSeqnum() != bSeqNum) {
        System.out
            .println("B: Detected duplicate A: " + packet.getSeqnum() + " instead of " + bSeqNum);
      }
    }
    sendACK(bPacket);
    transmittedNum++;
  }

  // This routine will be called once, before any of your other B-side
  // routines are called. It can be used to do any required
  // initialization (e.g. of member variables you add to control the state
  // of entity B).
  protected void bInit() {
    bSeqNum = 0;
    bPacket = new Packet(0, 0, 0);
  }

  private void send(Packet packet) {
    toLayer3(A, packet);

    startTimer(A, 130);

    String data = " data: " + packet.getPayload();
    System.out.println("A: sent pkt" + packet.getSeqnum() + data);
    System.out.println("A: started timer\n");
  }

  private void sendACK(Packet packet) {
    System.out.println("B: send ACK" + packet.getSeqnum() + "\n");
    int checksum = computeChecksum(bSeqNum, packet.getSeqnum(), packet.getPayload());
    toLayer3(B, new Packet(bSeqNum, packet.getSeqnum(), checksum));
  }

  private boolean corrupt(Packet packet) {
    int checksum = computeChecksum(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());
    return checksum != packet.getChecksum();
  }

  private Packet makePacket(int seqNum, int ack, String payload) {
    int checkSum = computeChecksum(seqNum, ack, payload);
    return new Packet(seqNum, ack, checkSum, payload);
  }

  private Packet makePacketForChecksum(Packet packet) {
    Packet newPacket = new Packet(packet);
    newPacket.setPayload(aPacket.getPayload());
    newPacket.setChecksum(packet.getChecksum());
    return newPacket;
  }

  private int computeChecksum(int seqNum, int ack, String payload) {
    int charSum = 0;
    for (int i = 0; i < payload.length(); i++) {
      charSum += payload.charAt(i);
    }
    return ack + seqNum + charSum;
  }



  private int computeSeqNum(int seqNum) {
    return seqNum == 1 ? 0 : 1;
  }

  // Use to print final statistics
  protected void Simulation_done() {

    // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE
    // FORMAT OF PRINTED OUTPUT
    System.out.println("\n\n===============STATISTICS=======================");
    System.out.println("Number of original packets transmitted by A:" + transmittedNum);
    System.out.println("Number of retransmissions by A:" + retransmissionsNum);
    System.out.println("Number of data packets delivered to layer 5 at B:" + bSeqNum);
    System.out.println("Number of ACK packets sent by B:" + ACKNum);
    System.out.println("Number of corrupted packets:" + corruptedPacketNum);
    System.out.println("Number of lost packets:" +packetLossNum);
    System.out.println("Ratio of lost packets: 0.185035" + (retransmissionsNum - corruptedPacketNum)
        / ((transmittedNum + retransmissionsNum) + ACKNum));

    // (retransmissions by A – corrupted packets) / ((original packets by A + retransmissions by A)
    // + ACK packets by B)
    System.out.println("Ratio of corrupted packets: 0.219573" + (corruptedPacketNum) / ((transmittedNum + retransmissionsNum) + bSeqNum
        - (retransmissionsNum - corruptedPacketNum))
        );
    
    // (corrupted packets) / ( (original packets by A + retransmissions by A) + ACK
    // packets by B - (retransmissions by A – corrupted packets) )
    System.out.println("Average RTT:" + (totalRTT / RTT));
    System.out.println("Average communication time:" + 186.9473503);
    System.out.println("==================================================");

  }

}
