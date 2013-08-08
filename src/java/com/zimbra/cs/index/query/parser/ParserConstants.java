/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
/* Generated By:JJTree&JavaCC: Do not edit this line. ParserConstants.java */
package com.zimbra.cs.index.query.parser;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
interface ParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int LPAREN = 3;
  /** RegularExpression Id. */
  int RPAREN = 4;
  /** RegularExpression Id. */
  int AND = 5;
  /** RegularExpression Id. */
  int OR = 6;
  /** RegularExpression Id. */
  int NOT = 7;
  /** RegularExpression Id. */
  int PLUS = 8;
  /** RegularExpression Id. */
  int MINUS = 9;
  /** RegularExpression Id. */
  int TERM = 10;
  /** RegularExpression Id. */
  int _TERM_START_CHAR = 11;
  /** RegularExpression Id. */
  int _TERM_CHAR = 12;
  /** RegularExpression Id. */
  int QUOTED_TERM = 13;
  /** RegularExpression Id. */
  int _ESCAPED_QUOTE = 14;
  /** RegularExpression Id. */
  int BRACED_TERM = 15;
  /** RegularExpression Id. */
  int CONTENT = 16;
  /** RegularExpression Id. */
  int SUBJECT = 17;
  /** RegularExpression Id. */
  int MSGID = 18;
  /** RegularExpression Id. */
  int ENVTO = 19;
  /** RegularExpression Id. */
  int ENVFROM = 20;
  /** RegularExpression Id. */
  int CONTACT = 21;
  /** RegularExpression Id. */
  int TO = 22;
  /** RegularExpression Id. */
  int FROM = 23;
  /** RegularExpression Id. */
  int CC = 24;
  /** RegularExpression Id. */
  int TOFROM = 25;
  /** RegularExpression Id. */
  int TOCC = 26;
  /** RegularExpression Id. */
  int FROMCC = 27;
  /** RegularExpression Id. */
  int TOFROMCC = 28;
  /** RegularExpression Id. */
  int IN = 29;
  /** RegularExpression Id. */
  int UNDER = 30;
  /** RegularExpression Id. */
  int FILENAME = 31;
  /** RegularExpression Id. */
  int TAG = 32;
  /** RegularExpression Id. */
  int MESSAGE = 33;
  /** RegularExpression Id. */
  int MY = 34;
  /** RegularExpression Id. */
  int AUTHOR = 35;
  /** RegularExpression Id. */
  int TITLE = 36;
  /** RegularExpression Id. */
  int KEYWORDS = 37;
  /** RegularExpression Id. */
  int COMPANY = 38;
  /** RegularExpression Id. */
  int METADATA = 39;
  /** RegularExpression Id. */
  int FIELD = 40;
  /** RegularExpression Id. */
  int _FIELD1 = 41;
  /** RegularExpression Id. */
  int _FIELD2 = 42;
  /** RegularExpression Id. */
  int DATE = 43;
  /** RegularExpression Id. */
  int MDATE = 44;
  /** RegularExpression Id. */
  int DAY = 45;
  /** RegularExpression Id. */
  int WEEK = 46;
  /** RegularExpression Id. */
  int MONTH = 47;
  /** RegularExpression Id. */
  int YEAR = 48;
  /** RegularExpression Id. */
  int AFTER = 49;
  /** RegularExpression Id. */
  int BEFORE = 50;
  /** RegularExpression Id. */
  int APPT_START = 51;
  /** RegularExpression Id. */
  int APPT_END = 52;
  /** RegularExpression Id. */
  int CONV_START = 53;
  /** RegularExpression Id. */
  int CONV_END = 54;
  /** RegularExpression Id. */
  int SIZE = 55;
  /** RegularExpression Id. */
  int BIGGER = 56;
  /** RegularExpression Id. */
  int SMALLER = 57;
  /** RegularExpression Id. */
  int CONV = 58;
  /** RegularExpression Id. */
  int CONV_COUNT = 59;
  /** RegularExpression Id. */
  int CONV_MINM = 60;
  /** RegularExpression Id. */
  int CONV_MAXM = 61;
  /** RegularExpression Id. */
  int MODSEQ = 62;
  /** RegularExpression Id. */
  int PRIORITY = 63;
  /** RegularExpression Id. */
  int IS = 64;
  /** RegularExpression Id. */
  int INID = 65;
  /** RegularExpression Id. */
  int UNDERID = 66;
  /** RegularExpression Id. */
  int HAS = 67;
  /** RegularExpression Id. */
  int TYPE = 68;
  /** RegularExpression Id. */
  int ATTACHMENT = 69;
  /** RegularExpression Id. */
  int ITEM = 70;
  /** RegularExpression Id. */
  int SORTBY = 71;
  /** RegularExpression Id. */
  int SORT = 72;

  /** Lexical state. */
  int TEXT = 0;
  /** Lexical state. */
  int DEFAULT = 1;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"(\"",
    "\")\"",
    "<AND>",
    "<OR>",
    "<NOT>",
    "\"+\"",
    "\"-\"",
    "<TERM>",
    "<_TERM_START_CHAR>",
    "<_TERM_CHAR>",
    "<QUOTED_TERM>",
    "\"\\\\\\\"\"",
    "<BRACED_TERM>",
    "\"content:\"",
    "\"subject:\"",
    "\"msgid:\"",
    "\"envto:\"",
    "\"envfrom:\"",
    "\"contact:\"",
    "\"to:\"",
    "\"from:\"",
    "\"cc:\"",
    "\"tofrom:\"",
    "\"tocc:\"",
    "\"fromcc:\"",
    "\"tofromcc:\"",
    "\"in:\"",
    "\"under:\"",
    "\"filename:\"",
    "\"tag:\"",
    "\"message:\"",
    "\"my:\"",
    "\"author:\"",
    "\"title:\"",
    "\"keywords:\"",
    "\"company:\"",
    "\"metadata:\"",
    "<FIELD>",
    "<_FIELD1>",
    "<_FIELD2>",
    "\"date:\"",
    "\"mdate:\"",
    "\"day:\"",
    "\"week:\"",
    "\"month:\"",
    "\"year:\"",
    "\"after:\"",
    "\"before:\"",
    "\"appt-start:\"",
    "\"appt-end:\"",
    "\"conv-start:\"",
    "\"conv-end:\"",
    "\"size:\"",
    "<BIGGER>",
    "\"smaller:\"",
    "\"conv:\"",
    "\"conv-count:\"",
    "\"conv-minm:\"",
    "\"conv-maxm:\"",
    "\"modseq:\"",
    "\"priority:\"",
    "\"is:\"",
    "\"inid:\"",
    "\"underid:\"",
    "\"has:\"",
    "\"type:\"",
    "\"attachment:\"",
    "\"item:\"",
    "\"sortby:\"",
    "\"sort:\"",
    "\"\\r\"",
  };

}
