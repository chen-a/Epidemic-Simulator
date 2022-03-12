// epidemic.java
/* Program that simulates an epidemic
 * author Andy Chen
 * version April 5, 2021
 *
 * Note:  This solution to MP9 is based on the posted solution to MP8
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;

/** Error reporting framework
 *  All error messages go to System.err (aka stderr, the standard error stream).
 *  Currently, this only supports fatal error reporting.
 *  Later it would be nice to have a way to report non-fatal errors.
 */
class Error {
    private static int warningCount = 0;

    /** Report a fatal error
     *  @param msg -- error message to be output
     *  This never returns, the program terminates reporting failure.
     */
    public static void fatal(String msg) {
        System.err.println("Epidemic: " + msg);
        System.exit(1); // abnormal termination
    }

    /** Non-fatal warning
     *  @param msg -- the warning message
     *  keeps a running count of warnings
     */
    public static void warn(String msg) {
        System.err.println("Warning: " + msg);
        warningCount = warningCount + 1;
    }

    /** Error exit if any warnings
     */
    public static void exitIfWarnings(String msg) {
        if (warningCount > 0) fatal(msg);
    }
}

/** Support for scanning input files with error reporting
 *  @see Error
 *  @see java.util.Scanner
 *  Ideally, this would be extend class Scanner, but class Scanner is final
 *  Therefore, this is a wrapper class around class Scanner
 */
class MyScanner {
    private Scanner sc; // the scanner we are wrapping

    public MyScanner(File f) throws FileNotFoundException {
        sc = new Scanner(f);
    }

    // methods that we wish we could inhereit from Scanner
    public boolean hasNext() {
        return sc.hasNext();
    }
    public boolean hasNext(String s) {
        return sc.hasNext(s);
    }
    public String next() {
        return sc.next();
    }

    // patterns that matter here

    // delimiters are spaces, tabs, newlines and carriage returns
    private static final Pattern delimPat = Pattern.compile("[ \t\n\r]*");

    // note that all of the following patterns allow an empty string to match
    // this is used in error detection below

    // if it's not a name, it begins with a non-letter
    private static final Pattern NotNamePat
        = Pattern.compile("([^A-Za-z]*)|");

    // names consist of a letter followed optionally by letters or digits
    private static final Pattern namePat
        = Pattern.compile("([A-Za-z][0-9A-Za-z]*)|");

    // if it's not an int, it begins with a non-digit, non-negative-sign
    private static final Pattern NotIntPat
        = Pattern.compile("([^-0-9]*)|");

    // ints consist of an optional sign followed by at least one digit
    private static final Pattern intPat = Pattern.compile(
        "((-[0-9]|)[0-9]*)"
    );

    // floats consist of an optional sign followed by
    // at least one digit, with an optional point before between or after them
    private static final Pattern floatPat = Pattern.compile(
        "-?(([0-9]+\\.[0-9]*)|(\\.[0-9]+)|([0-9]*))"
    );

    /** tool to defer computation of messages output by methods of MyScanner
     *  To pass a specific message, create a subclass of Message to do it
     *  In general, this will be used to create lambda expressions, so
     *  users will not need to even know the class name!
     */
    public interface Message {
        String myString();
    }

    // new methods added to class Scanner

    /** get the next nae from the scanner or complain if missing
     *  See namePat for the details of what makes a float.
     *  @param defalt  -- return value if there is no next item
     *  @param errorMesage -- the message to complain with (lambda expression)
     *  @return the next item or the defalt
     */
    public String getNextName(String defalt, Message errorMessage) {
        // first skip the delimiter, accumulate anything that's not a name
        String notName = sc.skip(delimPat).skip(NotNamePat).match().group();

        // second accumulate the name
        String name = sc.skip(namePat).match().group();

        if (!notName.isEmpty()) { // there's something else a name belonged
            Error.warn(
                errorMessage.myString() + ": name expected, skipping " + notName
            );
        }

        if (name.isEmpty()) { // missing name
            Error.warn(errorMessage.myString());
            return defalt;
        } else { // there was a name
            return name;
        }
    }

    /** get the next integer from the scanner or complain if missing
     *  See intPat for the details of what makes a float.
     *  @param defalt  -- return value if there is no next integer
     *  @param errorMesage -- the message to complain with (lambda expression)
     *  @return the next integer or the defalt
     */
    public int getNextInt(int defalt, Message errorMessage) {
        // first skip the delimiter, accumulate anything that's not an int
        String notInt = sc.skip(delimPat).skip(NotIntPat).match().group();

        // second accumulate the int, if any
        String text = sc.skip(delimPat).skip(intPat).match().group();

        if (!notInt.isEmpty()) { // there's something else where an int belonged
            Error.warn(
                errorMessage.myString() + ": int expected, skipping " + notInt
            );
        }

        if (text.isEmpty()) { // missing name
            Error.warn(errorMessage.myString());
            return defalt;
        } else { // the name was present and it matches intPat
            return Integer.parseInt(text);
        }
    }

    /** get the next float(double) from the scanner or complain if missing
     *  See floatPat for the details of what makes a float.
     *  @param defalt  -- return value if there is no next integer
     *  @param defalt  -- return value if there is no next float
     *  @param errorMesage -- the message to complain with (lambda expression)
     *  @return the next float or the defalt
     */
    public double getNextFloat(double defalt, Message errorMessage) {
        // skip the delimiter, if any, then the float, if any; get the latter
        String text = sc.skip(delimPat).skip(floatPat).match().group();

        if (text.isEmpty()) { // missing name
            Error.warn(errorMessage.myString());
            return defalt;
        } else { // the name was present and it matches intPat
            return Float.parseFloat(text);
        }
    }

    // patterns for use with the NextLiteral routines
    public static final Pattern beginParen = Pattern.compile("\\(|");
    public static final Pattern endParen = Pattern.compile("\\)|");
    public static final Pattern dash = Pattern.compile("-|");
    public static final Pattern semicolon = Pattern.compile(";|");

    /** try to get the next literal from the scanner
     *  @param literal -- the literal to get
     *  @returns true if the literal was present and skipped, false otherwise
     *  The literal parameter must be a pattern that can match the empty string
     *  if the desired literal is not present.
     */
    public boolean tryNextLiteral(Pattern literal) {
        sc.skip(delimPat); // allow delimiter before literal!
        String s = sc.skip(literal).match().group();
        return !s.isEmpty();
    }

    /** get the next literal from the scanner or complain if missing
     *  @param literal -- the literal to get
     *  @param errorMesage -- the message to complain with (lambda expression)
     *  @see tryNextLiteral for the mechanism used.
     */
    public void getNextLiteral(Pattern literal, Message errorMessage) {
        if (!tryNextLiteral(literal)) {
            Error.warn(errorMessage.myString());
        }
    }
}

/** Class for semantic error checkers
 *  @see Error
 *  This is a place to put error checking code that doesn't fit elsewhere.
 *  The error check methods here actually take up more space than the
 *  code they helped clarify, so the net gain in readability for this code
 *  is rather limited.  Perhaps as the program grows, they'll help more.
 */
class Check {
    private Check() {} // nobody should ever construct a check object

    /** Force a floating (double) value to be positive
     *  @param v -- value to check
     *  @param d -- default value to use if the check fails
     *  @param m -- message to output if check fails
     *  @return either value if success or defalt if failure
     */
    public static double positive(double v, double d, MyScanner.Message m) {
        if (v > 0.0) {
            return v;
        } else {
            Error.warn(m.myString());
            return d;
        }
    }

    /** Force a floating (double) value to be non negative
     *  @param v -- value to check
     *  @param d -- default value to use if the check fails
     *  @param m -- message to output if check fails
     *  @return either value if success or defalt if failure
     */
    public static double nonNeg(double v, double d, MyScanner.Message m) {
        if (v >= 0.0) {
            return v;
        } else {
            Error.warn(m.myString());
            return d;
        }
    }

    /** Scan end of command line containing a positive integer argument
     *  @param in -- the scanner to use
     *  @param msg -- the error message prefix to output if error
     *  @return the value scanned or 1 if the value was defective
     */
    public static int posIntSemicolon(MyScanner in , MyScanner.Message msg) {
        final int num = in .getNextInt(
            1, () -> msg + ": missing integer"
        );
        in .getNextLiteral(
            MyScanner.semicolon, () -> msg.myString() + num + ": missing ;"
        );

        if (num <= 0) {
            Error.warn(msg.myString() + num + ": not positive");
            return 1;
        }
        return num;
    }
}

/** Wrapper extending class Random, turning it into a singleton class
 *  @see Random
 *  Ideally, no user should ever create an instance of Random, all use this!
 *  Users can call MyRandom.stream.anyMethodOfRandom() (or of MyRandom)
 *              or MyRandom.stream().anyMethodOfRandom()
 *  Users can allocate MyRandom myStream = MyRandom.stream;
 *                  or MyRandom myStream = MyRandom.stream();
 *  No matter how they do it, they get the same stream
 */
class MyRandom extends Random {
    /** the only random number stream
     */
    public static final MyRandom stream = new MyRandom(); // the only stream;

    // nobody can construct a MyRandom except the above line of code
    private MyRandom() {
        super();
    }

    /* alternative access to the only random number stream
     * @return the only stream
     */
    public static MyRandom stream() {
        return stream;
    }

    // add distributions that weren't built in

    /** exponential distribution
     *  @param mean -- the mean value of the distribution
     *  @return a positive exponentially distributed random value
     */
    public double nextExponential(double mean) {
        return mean * -Math.log(this.nextDouble());
    }

    /** log-normal distribution
     *  @param median -- the median value of the distribution
     *  @param sigma  -- the sigma of the underlying normal distribution
     *  @return a log-normally distributed random value
     */
    public double nextLogNormal(double median, double sigma) {
        return Math.exp(sigma * this.nextGaussian()) * median;
    }
}

/** All about simulated time
 */
class Time {
    /** one second of simulated time */
    public static double second = 1.0F;

    /** one minute of simulated time */
    public static double minute = 60.0F * second;

    /** one hour of simulated time */
    public static double hour = 60.0F * minute;

    /** one day of simulated time */
    public static double day = 24.0F * hour;
}

/** Framework for discrete event simulation
 */
class Simulator {
    private Simulator() {} // prevent construction of instances!  Don't call!

    /** Functional interface for scheduling actions to be done later
     *  Users will generally never mention Action or trigger because
     *  this is used to support lambda expressions passed to schedule().
     */
    public static interface Action {
        void trigger(double time);
    }

    private static class Event {
        public final double time; // when will this event occur
        public final Action act; // what to do then
        public Event(double t, Action a) {
            time = t;
            act = a;
        }
    }

    private static final PriorityQueue < Event > eventSet
        = new PriorityQueue < > (
            (Event e1, Event e2) -> Double.compare(e1.time, e2.time)
        );

    /** Schedule an event to occur at a future time
     *  @param t, the time of the event
     *  @param a, what to do for that event
     *  example:
     *  <pre>
     *    Simulator.schedule( now+later, (double t)-> whatToDo( then, stuff ) );
     *  </pre>
     */
    public static void schedule(double t, Action a) {
        eventSet.add(new Event(t, a));
    }

    /** Run the simulation
     *  Before running the simulation, schedule the initial events
     *  all of the simulation occurs as side effects of scheduled events
     */
    public static void run() {
        while (!eventSet.isEmpty()) {
            Event e = eventSet.remove();
            e.act.trigger(e.time);
        }
    }

    /** Schedule everyone's activities throughout their day
     *  @author Andy Chen, Blake Thorson
     *  @param d the end of the simulation
     */
    public static void scheduleActivities(double d) {
        for (Person p: Person.allPeople) {
            for (double day = 0; day < d; day++) {
                p.dailySchedule(day);
            }
        }
    }
}

/** Places that people are associate with and may occupy.
 *  Every place is an instance of some kind of PlaceKind
 *  @see PlaceKind for most of the attributes of places
 */
class Place {
    // instance variables fixed at creation
    public final PlaceKind kind; // what kind of place is this?
    private final double transmissivity; // how dangerous is it to stay here

    // instance variables that vary with circumstances
    private int contageous = 0; // how many infectious people are here
    private final LinkedList < Person > occupants = new LinkedList < > ();

    /** Construct a new place
     *  @param k -- the kind of place
     *  @param t -- the transmissivity of the place
     */
    public Place(PlaceKind k, Double t) {
        kind = k;
        transmissivity = t;
    }

    /** a person arrives at a place
     *  @param p the person involved
     */
    void arrive(Person p) {
        occupants.add(p);
        p.location = this;
        // BUG: what else?
    }

    /** a person departs from a place
     *  @param p the person involved
     */
    void depart(Person p) {
        occupants.remove(p);
        p.location = null;
        // BUG: what else?
    }

    /** a person in this place changes contageon state
     *  @param time at which contageon change happens
     *  @param c, +1 means became contageous, -1 means recovered or died
     *  Note: when an infectious person arrives, always call contgeous(+1)
     *  before calling arrive() and always call contageous(-1) before
     *  calling depart() when a sick person leaves.
     */
    void contageous(double time, int c) {
        contageous = contageous + c;

        // when the number of contageous people in a place changes,
        for (Person p: occupants) {
            p.scheduleInfect(time, 1 / (contageous * transmissivity));
        }
    }

    /** Reschedule a peron's infection time after they move places
     *  @param time -- the time the person arrives at the new place
     *  @param p -- the person
     */
    void scheduleInfectPerson(double time, Person p) {
        if (time != 0) {
            p.scheduleInfect(time, 1 / (contageous * transmissivity));
        }
    }
}

/** Categories of places
 *  @see Place
 */
class PlaceKind {

    // linkage from person to associated place involves a schedule
    private class PersonSchedule {
        public Person person;
        public Schedule schedule;
        public PersonSchedule(Person p, Schedule s) {
            person = p;
            schedule = s;
        }
    }

    // instance variables from the input
    final String name; // the name of this category of place
    private double median; // median population for this category
    private double scatter; // scatter of size distribution, reduces to sigma
    private double transmissivity; // how likely is disease transmission here

    // instance variables developed during model elaboration
    private double sigma; // sigma of the log normal population distribution
    private Place unfilledPlace = null; // a place of this kind being filled
    private int unfilledCapacity = 0; // capacity of unfilledPlace

    // a list of all the people associated with this kind of place
    private final LinkedList < PersonSchedule > people
        = new LinkedList < > ();

    // static variables used for categories of places
    private static LinkedList < PlaceKind > allPlaceKinds
        = new LinkedList < > ();
    private static final MyRandom rand = MyRandom.stream();

    /** Construct a new place category by scanning an input stream
     *  @param in -- the input stream
     *  The stream must contain the category name, and the parameters
     *  for a log-normal distribution for the sizes.
     *  All specifications end with a semicolon.
     */
    public PlaceKind(MyScanner in ) {

        name = in .getNextName("???", () -> "place with no name");
        median = in .getNextFloat(
            9.9999F,
            () -> "place " + name + ": not followed by median"
        );
        scatter = in .getNextFloat(
            9.9999F,
            () -> "place " + name + " " + median + ": not followed by scatter"
        );
        transmissivity = (1 / Time.hour) * in .getNextFloat(
            9.9999F,
            () -> "place " + name + " " + median + " " + scatter +
            ": not followed by transmissivity"
        ); // BUG: conversion factors this is given in per hour!!!
        in .getNextLiteral(
            MyScanner.semicolon,
            () -> this.describe() + ": missing semicolon"
        );

        // complain if the name is not unique
        if (findPlaceKind(name) != null) {
            Error.warn(this.describe() + ": duplicate name");
        }
        // force the median to be positive
        median = Check.positive(median, 1.0F,
            () -> this.describe() + ": non-positive median?"
        );
        // force the scatter to be positive or zero
        scatter = Check.nonNeg(scatter, 0.0F,
            () -> this.describe() + ": negative scatter?"
        );
        // force the transmissivity to be positive or zero
        transmissivity = Check.nonNeg(transmissivity, 0.0F,
            () -> this.describe() + ": negative scatter?"
        );

        sigma = Math.log((scatter + median) / median);
        allPlaceKinds.add(this); // include this in the list of all
    }

    /** Produce a reasonable textual description of this place
     *  @return the description
     *  This shortens many error messages
     */
    private String describe() {
        return "place " + name + " " + median + " " + scatter +
            " " + transmissivity;
    }

    /** Find or make a place of a particular kind
     *  @return the place
     *  This should be called when a person is to be linked to a place of some
     *  particular kind, potentially occupying a space in that place.
     */
    private Place findPlace() {
        if (unfilledCapacity <= 0) { // need to make a new place
            // make new place using a log-normal distribution for the size
            unfilledCapacity
                = (int) Math.round(rand.nextLogNormal(median, sigma));
            unfilledPlace = new Place(this, transmissivity);
        }
        unfilledCapacity = unfilledCapacity - 1;
        return unfilledPlace;
    }

    /** Add a person to the population of this kind of place
     *  @param p the new person
     *  @param s the associated schedule
     */
    public void populate(Person p, Schedule s) {
        people.add(new PersonSchedule(p, s));
    }

    /** Distribute the people from all PlaceKinds to their individual places
     *  Prior to this, each PlaceKind knows all the people that will be
     *  associated with places of that kind, a list constructed by populate().
     *  This calls findPlace to create or find places.
     */
    public static void distributePeople() {

        // for each kind of place
        for (PlaceKind pk: allPlaceKinds) {
            // shuffle its people to break correlations from people to places
            Collections.shuffle(pk.people, MyRandom.stream);

            // for each person, associate that person with a specific place
            for (PersonSchedule ps: pk.people) {
                ps.person.emplace(pk.findPlace(), ps.schedule);
            }
        }
    }

    /** Find a category of place, by name
     *  @param n -- the name of the category
     *  @return the PlaceKind with that name, or null if none has been defined
     */
    public static PlaceKind findPlaceKind(String n) {
        for (PlaceKind pk: allPlaceKinds) {
            if (pk.name.equals(n)) return pk;
        }
        return null; // category not found
    }
}

/** Tuple of start and end times used for scheduling people's visits to places
 */
class Schedule {
    // instance variables
    public final double startTime; // times are in seconds anno midnight
    public final double endTime;

    /** construct a new Schedule
     *  @param in -- the input stream
     *  @param context -- the context for error messages
     *  Syntax: (0.0-0.0)
     *  Meaning: (start-end) times given in hours from midnight
     *  The begin paren must just have been scanned from the input stream
     */
    public Schedule(MyScanner in , MyScanner.Message context) {

        // get start time of schedule
        final double st = in .getNextFloat(
            23.98F, () -> context.myString() + "(: not followed by start time"
        ); in .getNextLiteral(
            MyScanner.dash, () -> context.myString() + "(" + st +
            ": not followed by -"
        );
        // get end time of schedule
        final double et = in .getNextFloat(
            23.99F, () -> context.myString() + "(" + st +
            "-: not followed by end time"
        ); in .getNextLiteral(
            MyScanner.endParen,
            () -> context.myString() + "(" + st + "-" + et +
            ": not followed by )"
        );

        // check sanity constraints on start and end times
        if (st >= 24.00F) {
            Error.warn(
                context.myString() + "(" + st + "-" + et +
                "): start time is tomorrow"
            );
        }
        Check.nonNeg(st, 0.0F,
            () -> context.myString() + "(" + st + "-" + et +
            "): start time is yesterday"
        );
        if (st >= et) {
            Error.warn(
                context.myString() + "(" + st + "-" + et +
                "): times out of order"
            );
        }
        startTime = st * Time.hour;
        endTime = et * Time.hour;
    }

    /** contert a Schedule back to textual form
     *  @return the schedule as a string
     *  Syntax: (0.0-0.0)
     *  Meaning: (start-end) times given in hours from midnight
     */
    public String toString() {
        return "(" + startTime / Time.hour + "-" + endTime / Time.hour + ")";
    }
}

/** Statistical Description of the disease progress
 */
class InfectionRule {
    private final double median; // median of the distribution
    private final double sigma; // sigma of the distribution
    private final double recovery; // recovery probability

    private static final MyRandom rand = MyRandom.stream();

    /** construct a new InfectionRule
     *  @param in -- the input stream
     *  @param context -- the context for error messages
     */
    public InfectionRule(MyScanner in , MyScanner.Message context) {
        final double scatter;
        median = Time.day * in .getNextFloat(1.0,
            () -> context.myString() + ": median expected"
        );
        scatter = Time.day * in .getNextFloat(0.0,
            () -> context.myString() + " " + median + ": scatter expected"
        );
        if (! in .tryNextLiteral(MyScanner.semicolon)) {
            recovery = in .getNextFloat(0.0,
                () -> context.myString() + " " + median + " " + scatter +
                ": recovery probability expected"
            );
            if (! in .tryNextLiteral(MyScanner.semicolon)) Error.warn(
                context.myString() + " " + median + " " + scatter +
                " " + recovery + "semicolon expected"
            );
        } else {
            recovery = 0.0;
        }

        // sanity checks on the values
        Check.positive(median, 0.0,
            () -> context.myString() + " " + median + " " + scatter +
            " " + recovery + ": non-positive median?"
        );
        Check.nonNeg(scatter, 0.0,
            () -> context.myString() + " " + median + " " + scatter +
            " " + recovery + ": negative scatter?"
        );
        Check.nonNeg(recovery, 0.0,
            () -> context.myString() + " " + median + " " + scatter +
            " " + recovery + ": negative recovery probability?"
        );
        if (recovery > 1.0) {
            Error.warn(
                context.myString() + " " + median + " " + scatter +
                " " + recovery + ": recovery probability greater than zero?"
            );
        }

        // we do this up front so scatter is never seen again.
        sigma = Math.log((scatter + median) / median);
    }

    /** Toss the dice to see if someone recovers under the terms of this rule
     *  @return true if recovers, false if not
     */
    public boolean recover() {
        return rand.nextFloat() <= recovery;
    }

    /** Toss the dice to see how long this disease state lasts under this rule
     *  @return the time until the next change of disease state
     */
    public double duration() {
        return rand.nextLogNormal(median, sigma);
    }
}

/** People in the simulated community each have a role
 *  @see Person
 *  @see PlaceSchedule
 *  Roles create links from people to the categories of places they visit
 */
class Role {

    // linkage from role to associated place involves a schedule
    private class PlaceSchedule {
	    public PlaceKind placeKind;
	    public Schedule schedule;
	    public PlaceSchedule( PlaceKind p, Schedule s ) {
	        placeKind = p;
	        schedule = s;
	    }
    }

    // instance variables
    public final String name; // name of this role
    private final LinkedList<PlaceSchedule> placeKinds = new LinkedList<>();

    private double fraction;  // fraction of the population in this role
    private int number;       // number of people in this role

    // static variables used for summary of all roles
    private static double sum = 0.0F; // sum of all the fractions
    private static LinkedList<Role> allRoles = new LinkedList<Role>();

    /** Construct a new role by scanning an input stream
     *  @param in -- the input stream
     *  The stream must contain the role name, and the number or fraction
     *  of the population in that role.
     *  All role specifications end with a semicolon.
     */
    public Role( MyScanner in ) {
	    PlaceKind homePlaceKind = null; // the home place for this role

	    name = in.getNextName( "???", ()-> "role with no name" );
	    fraction = in.getNextFloat(
	        9.9999F, ()-> "role " + name + ": not followed by population"
	    );

	    // get the list of places associated with this role
        boolean hasNext = in.hasNext(); // needed below for missing semicolon
	    while (hasNext && !in.tryNextLiteral( MyScanner.semicolon )) {

	        String placeName = in.getNextName( "???",
                ()->"role " + name + " " + fraction + ": place name expected"
	        );
	        PlaceKind pk = PlaceKind.findPlaceKind( placeName );
	        Schedule s = null;

	        // is placeName followed a schedule?
	        if (in.tryNextLiteral( MyScanner.beginParen )) {
		        s = new Schedule(
		            in, ()-> this.describe() + " " + placeName
		        );
	        }

	        // was it a real place name?
	        if (pk == null) {
		        Error.warn(
                    this.describe() + " " + placeName + ": undefined place?"
		       );
	        }

	        // see if this role is already associated with PlaceKind pk
	        boolean duplicated = false;
	        if (pk != null) {
		        if (pk == homePlaceKind) duplicated = true;
		        for (PlaceSchedule ps: placeKinds) {
		            if (ps.placeKind == pk) duplicated = true;
                }
	        }
	        if (duplicated) {
		        Error.warn(
                    this.describe() + " " + placeName + ": place name reused?"
		        );
	        } else { // only record non-duplicate entries
                placeKinds.add( new PlaceSchedule( pk, s ) );  // schedule all
		        if (s == null) {
		            if (homePlaceKind != null) Error.warn(
                        this.describe() + " " + placeName + ": a second home?"
		            );
		        homePlaceKind = pk;
		        }
	        }
	        hasNext = in.hasNext();
	    }
        if (!hasNext) Error.warn(this.describe() + ": missing semicolon?");

	    // complain if the name is not unique
	    if (findRole( name ) != null) {
	        Error.warn( this.describe() + ": role name reused?" );
	    }
	    // force the fraction or population to be positive
	    fraction = Check.positive( fraction, 0.0F,
	        ()-> this.describe() + ": negative population?"
	    );
	    sum = sum + fraction;

	    // complain if no places for this role
	    if (homePlaceKind == null) {
	       Error.warn( this.describe() + ": no home specified?" );
	    }
	    if (placeKinds.isEmpty()) {
	        Error.warn( this.describe() + ": has no places?" );
	    }
	    // complain if two events overlap
	    if (overlappingSchedules()) {
            Error.warn (this.describe() + ": has overlapping schedule times?");
        }

	    allRoles.add( this ); // include this role in the list of all roles
    }

    /** Produce a reasonably full textual description of this role
     *  @return the description
     *  This shortens many error messages
     */
    private String describe() {
	    return "role " + name + " " + fraction;
    }

    /** Check if the person's schedule has overlapping times
     *  @author Andy Chen
     *  @return true if there is or false if there isn't
     */
    private boolean overlappingSchedules() {
        for (PlaceSchedule p1: placeKinds) {
            for (PlaceSchedule p2: placeKinds) {
                if (p1 != p2) {
                    if ((p1.schedule != null) && (p2.schedule != null)) {
                        double p1Start = p1.schedule.startTime;
                        double p1End = p1.schedule.endTime;
                        double p2Start = p2.schedule.startTime;
                        double p2End = p2.schedule.endTime;
                        if ((p1Start <= p2End) && (p1End >= p2Start)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Find a role, by name
     *  @param n -- the name of the role
     *  @return the role with that name, or null if none has been defined
     */
    private static Role findRole( String n ) {
	    for (Role r: allRoles) {
	        if (r.name.equals( n )) return r;
	    }
	    return null; // role not found
    }

    /** Create the total population, divided up by roles in
     *  @param population -- the total population to be created
     *  @param infected -- the total number of initially infected people
     *  The math here divides the population in the ratio of the numbers
     *  given for each role.
     *  It is critical that this not be done until all roles are known.
     */
    public static void populateRoles( int population, int infected ) {
        int pop = population; // working copy used only in infection decisions
        int inf = infected;   // working copy used only in infection decisions
        final MyRandom rand = MyRandom.stream;

	    if (allRoles.isEmpty()) Error.fatal( "no roles specified" );
	    for (Role r: allRoles) {
	        // how many people are in this role
	        r.number = (int)Math.round( (r.fraction / r.sum) * population );

	        // make that many people and infect the right number at random
	        for (int i = 0; i < r.number; i++) {
		        Person p = new Person( r );

	            // the ratio inf/pop is probability this person is infected
                if (rand.nextFloat() < ((float)inf / (float)pop)) {
		            p.infect( 0.0 );
		            inf = inf - 1;
		        }
		        pop = pop - 1;

	            // each person is associated all their role's place kinds
		        // note that this does not create places yet
		        for (PlaceSchedule ps: r.placeKinds) {
		            ps.placeKind.populate( p, ps.schedule );
		        }
	        }
	    }

	    // finish putting people in their places
	    // this actually creates the places and puts people in them
	    PlaceKind.distributePeople();
    }
}

/** People are the central actors in the simulation
 *  @see Role for the roles people play
 *  @see Place for the places people visit
 */
class Person {

    private static enum DiseaseStates {
        uninfected,
        latent,
        asymptomatic,
        symptomatic,
        bedridden,
        recovered,
        dead // this must be the last state so that
        // DiseaseStates.dead.ordinal()+1 is the number of disease states
    }

    // population broken down by disease state
    private static int[] popByState = new int[DiseaseStates.dead.ordinal() + 1];

    // timing characteristics of disease state
    private static InfectionRule latent;
    private static InfectionRule asymptomatic;
    private static InfectionRule symptomatic;
    private static InfectionRule bedridden;

    public static void setDiseaseParameters(
        InfectionRule l, InfectionRule a, InfectionRule s, InfectionRule b
    ) {
        latent = l;
        asymptomatic = a;
        symptomatic = s;
        bedridden = b;
    }

    // linkage from person to place involves a schedule
    private class PlaceSchedule {
        public Place place;
        public Schedule schedule;
        public PlaceSchedule(Place p, Schedule s) {
            place = p;
            schedule = s;
        }
    }

    // instance variables created from model description
    private final Role role; // role of this person
    private Place home; // this person's home place, set by emplace
    private final LinkedList < PlaceSchedule > places = new LinkedList < > ();

    // instance variables that change as simulation progressses
    private DiseaseStates diseaseState = DiseaseStates.uninfected;
    public Place location; // initialized by emplace
    private double infectMeTime = 0.0; // time I will get infected
    // for the above, the default 0.0 allows for infection at startup

    // static variables used for all people
    public static LinkedList < Person > allPeople
        = new LinkedList < Person > ();
    private static MyRandom rand = MyRandom.stream;

    /** Construct a new person to perform some role
     *  @param r -- the role
     *  This constructor deliberately defers putting people in any places
     */
    public Person(Role r) {
        role = r;
        allPeople.add(this); // include this person in the list of all
        popByState[diseaseState.ordinal()]++; // include it in the statistics
    };

    // methods used during model construction, at time 0.0

    /** Associate this person to a particular place
     *  @param p -- the place
     *  @param s -- the associated schedule
     */
    public void emplace(Place p, Schedule s) {
        if (s != null) {
            places.add(new PlaceSchedule(p, s));
        } else {
            assert home == null: "Role guarantees only one home place";
            home = p;
            location = home;

            location.arrive(this); // tell the location about new occupant
        }
    }

    // simulation of behavior

    /** Schedule the person's activity in each day
     *  @author Andy Chen, Blake Thorson
     *  @param day -- the day of the simulation
     */
    public void dailySchedule(double day) {
        if (diseaseState != DiseaseStates.bedridden
            && diseaseState != DiseaseStates.dead) {
            for (PlaceSchedule ps: places) {
                Simulator.schedule(day * Time.day + ps.schedule.startTime,
                    (double t) -> move(t, ps.place)
                );
                Simulator.schedule(day * Time.day + ps.schedule.endTime,
                    (double t) -> move(t, home)
                );
            }
        }
    }

    /** Move the person to their next place
     *  @author Andy Chen
     *  @param time -- the time the person goes at
     *  @param p -- the place the person goes to
     */
    public void move(double time, Place p) {
        if (diseaseState != DiseaseStates.dead
            && diseaseState != DiseaseStates.bedridden) {
            boolean isContageous = false;
            if ((diseaseState == Person.DiseaseStates.asymptomatic) ||
                (diseaseState == Person.DiseaseStates.symptomatic) ||
                (diseaseState == Person.DiseaseStates.bedridden)) {
                isContageous = true;
            }
            if (isContageous) location.contageous(time, -1);
            location.depart(this);
            if (isContageous) p.contageous(time, 1);
            p.arrive(this);
        }
    }

    // simulation of behavior

    /** Schedule the time at which a person will be infected
     *  @param time -- the current time
     *  @param meanDelay -- the delay until infection
     */
    public void scheduleInfect( double time, double meanDelay ) {
        if (diseaseState == DiseaseStates.uninfected) { // irrelevant if not
            double delay = rand.nextExponential( meanDelay );
            infectMeTime = time + delay;
            Simulator.schedule( infectMeTime, (double t)-> infect( t ) );
        }
    }

    /** Infect this person
     *  @param now -- the time of infection
     *  This may be called on a person in any infection state and makes the
     *  person latent.
     *  This is a schedulable event service routine
     */
    public void infect(double now) {
        if ((diseaseState == DiseaseStates.uninfected) // no reinfection
            &&
            (infectMeTime == now) // if not rescheduled
        ) {
            final double duration = latent.duration();

            // update statistics
            popByState[diseaseState.ordinal()]--;
            diseaseState = DiseaseStates.latent;
            popByState[diseaseState.ordinal()]++;

            if (latent.recover()) {
                Simulator.schedule(now + duration, (double t) -> recover(t));
            } else {
                Simulator.schedule(
                    now + duration, (double t) -> beContageous(t)
                );
            }
        }
    }

    /** This person becomes contageous and asymptomatic
     *  @param time -- the time of this state change
     *  This may be called on a person in with a latent infection and makes the
     *  person asymptomatic.
     *  This is a schedulable event service routine
     */
    public void beContageous(double time) {
        assert diseaseState == DiseaseStates.latent: "not latent";
        final double duration = asymptomatic.duration();

        // update statistics
        popByState[diseaseState.ordinal()]--;
        diseaseState = DiseaseStates.asymptomatic;
        popByState[diseaseState.ordinal()]++;

        // tell place that I'm sick
        if (location != null) location.contageous(time, +1);

        if (asymptomatic.recover()) {
            Simulator.schedule(time + duration, (double t) -> recover(t));
        } else {
            Simulator.schedule(time + duration, (double t) -> feelSick(t));
        }
    }

    /** This person is contageous and starts feeling sick
     *  @param time -- the time of this state change
     *  This may be called on a person in with an asymptomatic infection and
     *  makes the person symptomatic.
     *  This is a schedulable event service routine
     */
    public void feelSick(double time) {
        assert diseaseState == DiseaseStates.asymptomatic: "not asymptomatic";
        final double duration = symptomatic.duration();

        // update statistics
        popByState[diseaseState.ordinal()]--;
        diseaseState = DiseaseStates.symptomatic;
        popByState[diseaseState.ordinal()]++;

        if (symptomatic.recover()) {
            Simulator.schedule(time + duration, (double t) -> recover(t));
        } else {
            Simulator.schedule(time + duration, (double t) -> goToBed(t));
        }
    }

    /** This person is contageous and feels so bad they go to bed
     *  @author Andy Chen
     *  @param time -- the time of this state change
     *  This may be called on a person in with an symptomatic infection and
     *  makes the person bedridden.
     *  This is a schedulable event service routine
     */
    public void goToBed(double time) {
        assert diseaseState == DiseaseStates.symptomatic: "not symptomatic";
        final double duration = bedridden.duration();

        // update statistics
        popByState[diseaseState.ordinal()]--;
        diseaseState = DiseaseStates.bedridden;
        popByState[diseaseState.ordinal()]++;

        if (symptomatic.recover()) {
            Simulator.schedule(time + duration, (double t) -> recover(t));
        } else {
            Simulator.schedule(time + duration, (double t) -> die(t));
        }
    }

    /** This person gets better
     *  @param time -- the time of this state change
     *  This may be called on a person in any infected disease state
     *  and leaves the person well and immune from further infection.
     *  This is a schedulable event service routine
     */
    public void recover(double time) {
        // update statistics
        popByState[diseaseState.ordinal()]--;
        diseaseState = DiseaseStates.recovered;
        popByState[diseaseState.ordinal()]++;

        if (location != null) location.contageous(time, -1);
    }

    /** This person dies
     *  @param time -- the time of this state change
     *  This may be called on a bedridden person and
     *  makes the person die.
     *  This is a schedulable event service routine
     */
    public void die(double time) {
        assert diseaseState == DiseaseStates.bedridden: "not bedridden";
        // update statistics
        popByState[diseaseState.ordinal()]--;
        diseaseState = DiseaseStates.dead;
        popByState[diseaseState.ordinal()]++;

        if (location != null) {
            location.contageous(time, -1);
            location.depart(this);
        }

        // no new event is scheduled.
    }

    // reporting tools

    /** Report population statistics at the given time
     *  @param time
     *  Intended to be scheduled as an event at time zero, initiates a
     *  sequence of daily reporting events.
     *  Each report is a CSV line giving the time and the population
     *  for each disease state.
     */
    public static void report(double time) {
        System.out.print(Double.toString(time / Time.day));
        for (int i = 0; i <= DiseaseStates.dead.ordinal(); i++) {
            System.out.print(",");
            System.out.print(Integer.toString(popByState[i]));
        }
        System.out.println();

        // schedule the next report
        Simulator.schedule(time + 24 * Time.hour,
            (double t) -> Person.report(t)
        );
    }

    /** Places everyone at their homes
     *  Only called during start of simulation
     *  @author Andy Chen, Blake Thorson
     */
    public static void startAtHome() {
        for (Person p: allPeople) {
            p.location = p.home;
            if (p.diseaseState == DiseaseStates.latent) {
                p.location.contageous(0,1);
            }
        }
    }

    /** Print out the entire population
     *  This is needed only in the early stages of debugging
     *  and obviously useless for large populations.
     */
    public static void printAll() {
        for (Person p: allPeople) {
            // line 1: person id and role
            System.out.print(p.toString());
            System.out.print(" ");
            System.out.println(p.role.name);

            // line 2 the home
            System.out.print(" "); // indent following lines
            System.out.print(p.home.kind.name);
            System.out.print(" ");
            System.out.print(p.home.toString());
            System.out.println();
            // lines 3 and up: each place and its schedule
            for (PlaceSchedule ps: p.places) {
                System.out.print(" "); // indent following lines
                System.out.print(ps.place.kind.name);
                System.out.print(" ");
                System.out.print(ps.place.toString());
                assert ps.schedule != null: "guaranteed by PlaceKind";
                System.out.print(ps.schedule.toString());
                System.out.println();
            }
        }
    }
}

/** The main class
 *  This class should never be instantiated.
 *  All methods here are static and all but the main method are private.
 *  @see Role for the framework that creates people
 *  @see PlaceKind for the framework from which places are constructed
 *  @see Person for the ultimate result of this creation
 */
public class epidemic {

    /** Read the details of the model from an input stream
     *  @param in -- the stream
     *  Identifies the keywords population, role, etc and farms out the
     *  work for most of these to the classes that construct model parts.
     *  The exception (for now) is the total population.
     */
    private static void buildModel(MyScanner in ) {
        int pop = 0; // the population of the model, 0 = uninitialized
        int infected = 0; // number initially infected, 0 = uninitialized
        double endOfTime = 0.0; // 0.0 = uninitialized

        // rules describing the progress of the infection
        InfectionRule latent = null;
        InfectionRule asymptomatic = null;
        InfectionRule symptomatic = null;
        InfectionRule bedridden = null;

        while ( in .hasNext()) { // scan the input file

            // each item begins with a keyword
            String keyword = in .getNextName("???", () -> "keyword expected");
            if ("population".equals(keyword)) {
                int p = Check.posIntSemicolon( in , () -> "population");
                if (pop != 0) {
                    Error.warn("population specified more than once");
                } else {
                    pop = p;
                }
            } else if ("infected".equals(keyword)) {
                int i = Check.posIntSemicolon( in , () -> "infected");
                if (infected != 0) {
                    Error.warn("infected specified more than once");
                } else {
                    infected = i;
                }
            } else if ("latent".equals(keyword)) {
                if (latent != null) {
                    Error.warn("latency time specified more than once");
                }
                latent = new InfectionRule( in , () -> "latent");
            } else if ("asymptomatic".equals(keyword)) {
                if (asymptomatic != null) {
                    Error.warn("asymptomatic time specified more than once");
                }
                asymptomatic = new InfectionRule( in , () -> "asymptomatic");
            } else if ("symptomatic".equals(keyword)) {
                if (symptomatic != null) {
                    Error.warn("symptomatic time specified more than once");
                }
                symptomatic = new InfectionRule( in , () -> "symptomatic");
            } else if ("bedridden".equals(keyword)) {
                if (bedridden != null) {
                    Error.warn("bedridden time specified more than once");
                }
                bedridden = new InfectionRule( in , () -> "bedridden");
            } else if ("end".equals(keyword)) {
                final double et = in .getNextFloat(1.0F,
                    () -> "time: end time missing"
                ); in .getNextLiteral(
                    MyScanner.semicolon, () -> "end " + et + ": missing ;"
                );
                Check.positive(et, 0.0F,
                    () -> "end " + et + ": negative end time?"
                );
                if (endOfTime > 0.0) {
                    Error.warn("end " + et + ": duplicate end time");
                } else {
                    endOfTime = et;
                }
            } else if ("role".equals(keyword)) {
                new Role( in );
            } else if ("place".equals(keyword)) {
                new PlaceKind( in );
            } else if (keyword == "???") { // there was no keyword
                // == is allowed here 'cause we're detecting the default value
                // we need to advance the scanner here or we'd stick in a loop
                if ( in .hasNext()) in .next();
            } else { // none of the above
                Error.warn("not a keyword: " + keyword);
            }
        }

        // check that all required fields are filled in

        if (pop == 0) Error.warn("population not given");
        if (latent == null) Error.warn("latency time not given");
        if (asymptomatic == null) Error.warn("asymptomatic time not given");
        if (symptomatic == null) Error.warn("symptomatic time not given");
        if (bedridden == null) Error.warn("bedridden time not given");
        if (endOfTime == 0.0) Error.warn("end of time not given");

        Error.exitIfWarnings("Aborted due to errors in input");

        Person.setDiseaseParameters(
            latent, asymptomatic, symptomatic, bedridden
        );

        Simulator.schedule( // schedule the end of time
            endOfTime * Time.day, (double t) -> System.exit(0)
        );

        // Role is responsible for figuring out how many people per role
        Role.populateRoles(pop, infected);

        // Schedule the first of the daily reports to be printed
        Simulator.schedule(0.0, (double t) -> Person.report(t));

        // Start everyone at their home and schedule their daily activities
        Person.startAtHome();
        Simulator.scheduleActivities(endOfTime);
    }

    /** The main method
     *  @param args -- the command line arguments
     *  Most of this code is entirely about command line argument processing.
     *  It calls buildModel and will eventuall also start the simulation.
     */
    public static void main(String[] args) {
        if (args.length < 1) Error.fatal("missing file name");
        if (args.length > 1) Error.warn("too many arguments: " + args[1]);
        try {
            buildModel(new MyScanner(new File(args[0])));
            // Person.printAll(); // BUG:  potentially useful for debugging
            Simulator.run();
        } catch (FileNotFoundException e) {
            Error.fatal("could not open file: " + args[0]);
        }
    }
}
