[![Release](https://jitpack.io/v/dilbertside/j8583.svg)](https://jitpack.io/#dilbertside/j8583)
[![Build Status](https://travis-ci.org/dilbertside/j8583.svg)](https://travis-ci.org/dilbertside/j8583)

Fork of Enrique Zamudio j8583 Java implementation of the ISO8583 protocol.

All information is available at http://j8583.sourceforge.net/ and on the wiki here at GitHub.

Purpose of this fork is to leave behind the j8583 cumbersome XML configuration.

It is Java 8 compliant and use some Java 8 DateTime additions such as:
 
   * java.time.ZonedDateTime
   * java.time.LocalDateTime
   * java.time.LocalDate
   * java.time.LocalTime

By going Pojo, some routine introspection will make ISO8583 values mapped to their primitive value and eventually nested pojo for composite fields.

A Pojo associated with Jackson will make the decoded ISO8583 frame available as JSON to be consumed by other means.

## Show me the code!

### Network Management POJO
```java
@Iso8583(type=0x800)
public class NetworkMgmtRequest {
  
  protected String bogus;
  
  @Iso8583Field(index=7, type=IsoType.DATE10)
  protected Date dateTransaction;
  
  @Iso8583Field(index=11, type=IsoType.NUMERIC, length=6)
  protected Number systemTraceAuditNumber;
  
  
  @Iso8583Field(index=70, type=IsoType.NUMERIC, length=3)
  protected Number codeInformationNetwork;
  //getter&setters removed form clarity 
  //...
}
```

### Setup
```java
  MessageFactoryPojo<IsoMessage> mf = ConfigParserPojo.createFromPojo(NetworkMgmtRequest.class, AcquirerFinancialRequest.class);
  mf.setCharacterEncoding(StandardCharsets.UTF_8.name());
  mf.setUseBinaryMessages(false);
  mf.setAssignDate(true);
```


### Encoding from POJO
```java
  Date dateTransaction = new GregorianCalendar(2000, 1, 1, 1 ,1, 1).getTime();
  Number systemTraceAuditNumber = 999999;
  Number codeInformationNetwork = 1;
  NetworkMgmtRequest request = new NetworkMgmtRequest(dateTransaction, systemTraceAuditNumber, codeInformationNetwork);
  //build Iso Message from POJO 
  IsoMessage iso = mf.newMessage(request);
  System.out.println(iso.debugString());
```
### Decoding from frame to POJO
```java
  //isoB is the byte frame received from network
  IsoMessage m = mf.parseMessage(isoB, 0);
  NetworkMgmtRequest nmr = mf.parseMessage(m, NetworkMgmtRequest.class);
  System.out.println(nmr.toString());
```
See for more

## To use it in your Maven build add:

```xml
<repositories>
  <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
</repositories>
```

and the dependency:

```xml
  <dependency>
    <groupId>com.github.dilbertside</groupId>
    <artifactId>j8583</artifactId>
    <version>2.12.0</version>
  </dependency>
```

## TODO

* More unit tests