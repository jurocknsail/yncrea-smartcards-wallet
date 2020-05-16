/**
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
 * 
 */

package com.sun.jcclassic.samples.walletsolution;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;


public class WalletSolution extends Applet {

	/* constants declaration */
	// code of CLA byte in the command APDU header
	final static byte Wallet_CLA = (byte) 0xB0;

	// codes of INS byte in the command APDU header
	final static byte CREDIT = (byte) 0x30;
	final static byte DEBIT = (byte) 0x40;
	final static byte GET_BALANCE = (byte) 0x50;

	// maximum balance
	final static short MAX_BALANCE = 0x7FFF;

	// maximum transaction amount
	final static byte MAX_TRANSACTION_AMOUNT = 127;


	// signal invalid transaction amount
	// amount > MAX_TRANSACTION_MAOUNT or amount < 0
	final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6A83;

	// signal that the balance exceed the maximum
	final static short SW_EXCEED_MAXIMUM_BALANCE = 0x6A84;

	// signal the balance becomes negative
	final static short SW_NEGATIVE_BALANCE = 0x6A85;

	/* instance variables declaration */
	short balance=0;

	private WalletSolution (byte[] bArray, short bOffset, byte bLength){
		    register();
	} // end of the constructor

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// create a Wallet applet instance
		new WalletSolution(bArray, bOffset, bLength);
	} // end of install method

	

	public void process(APDU apdu) {
		
		// APDU object carries a byte array (buffer) to
		// transfer incoming and outgoing APDU header
		// and data bytes between card and CAD
		// At this point, only the first header bytes
		// [CLA, INS, P1, P2, P3] are available in
		// the APDU buffer.
		// The interface javacard.framework.ISO7816
		// declares constants to denote the offset of
		// these bytes in the APDU buffer
		byte[] buffer = apdu.getBuffer();

		// check SELECT APDU command
		if ((buffer[ISO7816.OFFSET_CLA] == 0)
				&& (buffer[ISO7816.OFFSET_INS] == (byte) (0xA4)))
			return;

		// verify the reset of commands have the
		// correct CLA byte, which specifies the
		// command structure
		if (buffer[ISO7816.OFFSET_CLA] != Wallet_CLA)
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);

		switch (buffer[ISO7816.OFFSET_INS]) {
		case GET_BALANCE:
			getBalance(apdu);
			return;
		case DEBIT:
			debit(apdu);
			return;
		case CREDIT:
			credit(apdu);
			return;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	} // end of process method

	private void credit(APDU apdu) {

		byte[] buffer = apdu.getBuffer();
		// Lc byte denotes the number of bytes in the
		// data field of the command APDU
		byte numBytes = buffer[ISO7816.OFFSET_LC];

		// indicate that this APDU has incoming data
		// and receive data starting at the offset
		// ISO7816.OFFSET_CDATA following the 5 header
		// bytes.
		byte byteRead = (byte) (apdu.setIncomingAndReceive());

		// it is an error if the number of data bytes
		// read does not match the number in Lc byte
		if (byteRead != 1)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

		// get the credit amount
		byte creditAmount = buffer[ISO7816.OFFSET_CDATA];

		// check the credit amount
		if ((creditAmount > MAX_TRANSACTION_AMOUNT) || (creditAmount < 0))
			ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);

		// check the new balance
		if ((balance + creditAmount) > MAX_BALANCE)
			ISOException.throwIt(SW_EXCEED_MAXIMUM_BALANCE);

		// credit the amount
		balance = (short) (balance + creditAmount);

	} // end of deposit method

	private void debit(APDU apdu) {
		  
	    byte[] buffer = apdu.getBuffer();
	    byte numBytes = (byte)(buffer[ISO7816.OFFSET_LC]);
	    byte byteRead = (byte)(apdu.setIncomingAndReceive());
	  
	    if (byteRead != 1) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
	  
	    // get debit amount
	    byte debitAmount = buffer[ISO7816.OFFSET_CDATA];
	  
	    // check debit amount
	    if ( ( debitAmount > MAX_TRANSACTION_AMOUNT) || (debitAmount < 0 ) )
	      ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);
	  
	    // check the new balance
	    if ( ( balance - debitAmount) < 0 ) ISOException.throwIt(SW_NEGATIVE_BALANCE);
	  
	    balance = (short) (balance - debitAmount);
	    
  } // end of debit method 

	private void getBalance(APDU apdu) { 
		 
	    byte[] buffer = apdu.getBuffer();
	    // inform system that the applet has finished
	    // processing the command and the system should
	    // now prepare to construct a response APDU
	    // which contains data field
	    short ne = apdu.setOutgoing();
	  
	    if ( ne < 2 ) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
	  
	    //informs the CAD the actual number of bytes
	    //returned
	    apdu.setOutgoingLength((byte)2);
	  
	    // move the balance data into the APDU buffer
	    // starting at the offset 0
	    buffer[0] = (byte)(balance >> 8);
	    buffer[1] = (byte)(balance & 0xFF);
	  
	    // send the 2-balance byte at the offset
	    // 0 in the apdu buffer
	    apdu.sendBytes((short)0, (short)2);
  
  
  } // end of getBalance method

	
} // end of class Wallet
