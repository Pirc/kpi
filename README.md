# kpi
A library that uses Akka-based actors to gather and report KPI in real time to your admin app.

## Purpose

While contemplating creating an administrative dashboard for the Pirc back-end platform, I was thinking about what data I was going to show and trying to figure out how I would show it.  I was also wondering whether there were any third party applications that I would like to use to do my dashboarding.  The problem that I ran into was that my dashboard/admin app is both a place where other Pirc stakeholders would come to for current statistics and KPI's, and it was a place that I would want to go to be able to not only see the curent state of key system processes but also a place where I would want to perhaps kick off or otherwise manage other processes in the system.

Consider the following examples of things I need my admin app to do:

* **Counts:** how many subscribers do I have?  How many people have cancelled their subscription?  How many in the last hour, the last day, the last week?  How about a histogram of this data?
* **Ingest Activity:** One specific area of critical importance to the Pirc.com application is to be able to monitor ingest activity, and potentially to kick off new ingest jobs.
* **Pircular Activity:** The main point of Pirc.com is to send out the weekly "Pircular" email, a personalize email that contains sales on products that the user has indicated an interest in.  We track each Pircular that is sent out, and we also should be able to know which Pirculars are currently being created (a process which often involves ingest activity (see prior bullet point).  It would be good to know how many Pirculars have been sent and also who is currently being worked on and what the status is of each Pircular task.
* **Favorite-ing Activity:** How many favorites have been created this week?  Which brands are most often favorited.  This is useful not only from a stats and reporting perspective, but we could also use this data in real time to create "popular" lists.
* **Page View Tracking:** There are already many tools available for tracking page views, sessions, etc. from the front end.  What may be interesting on the back end though is to have the ability to know which deal details are being viewed most often so that we report to the community what deals are most popular this week.

The thing about all of these items is that they typically require one-off work, per metric, to get the counts and any other supporting information needed for the dashboard.  Even worse, as new applications are created, adding new measurements to the admin app doesn't get any easier.  Sometimes we will sometimes even need to execute queries against the production database to get at the numbers, which can have a performance impact on the production OLTP system.  And if you want to have more visibility you will need to have more queries running against the database.

What I really wanted to have was a way to quickly add new KPI's to the system that exerted little or no load on the application, that came with a default UI that would make the KPI's available to a generic administrative KPI inpector without needing to do any additional UI work, that could also serve as a place where administrative functions (e.g. launching a particular crawl or sending a test Pircular to a particular user) could be done in a clean way.  That is why I started coding the **KPI** package.

## What this Looks Like in Code

What I really want to be able to do in my source code is to add a line of code here or there when certain events happen that will, under the sheets, make the related metrics available to other applications (such as admin apps).  For exmaple, here is how I would like to bump a counter:

```
  KPI.tracker("/member/unsubscribe", Instance(member.getId))
```

This may happen on any of the application servers in the system, and the result will be that one counter somewhere in the distributed architecture will be notified and able to track, in-memory, what the current count of unsubscribed members is and will be able to report out standard counts (hourly, daily, weekly) of unsubscribe activity as well as the last _n_ members who unsubscribed.

In this example, `member.unsubscribe` is a "static counter," meaning that there is one instance of this counter in the system that is constantly present.  Another example of a tracker would be one that tracks the creation of an individual Pircular, or one that tracks a single instance of a deal ingest.  In this case, the code would look something like this:

```
   KPI.tracker(s"/member/pircular/_${member.getId}", Status("Starting Walgreens crawl"))
```

This will have the effect of sending a Status message to an "instance tracker".  Unlike the static counter, there may be many instances of an instance tracker in the system, and the special "_" character will indicate to the system that an instance tracker is required.  In this example, there may be a few dozen instances of the "/member/pircular" tracker running, one for each of the Pirculars being generated at any point in time, and the tracker itself will keep, say, the last 10 status messages that the application has emitted about this Pircular along with timestamps of when those messages came in and any other information that the application choosed to send in.

## Tracker Paths

You may have noticed that the _path_ argument, which is the first argument passed in to `KPI.tracker()` follows a hierarchical path-like structure, just like a filesystem or an Akka actor system.  When any application passes a _path_ into the `tracker` object, the system will use that path to attempt to locate that tracker.  It will do so by walking an tracker hierarchy.  All of the trackers are rooted in the main "master tracker".   If at any point in walking down the hierarchy we don't find a child at the specified path, a new one is created and we then walk through that child to get to the ultimately arrive at the correct tracker object.

The hierarchy is important.  This structure is what makes it possible for us to have a generic UI of KPI's that don't necessarily require new UI work when new KPI's are added to the system.  Imagine, for example, a file explorer type of interface where the "directories" may all be expanded and the individual "files" viewed.  As the application grows and becomes more complex, and there are more areas that you need visibility into you can simply add more calls to teh `KPI.tracker()` object and those new trackers are created on demand in the system and then are automatically visible in the generic tracker UI.

## Using the Trackers for Reporting

Thus far, we've looked at the logging side of the Tracker object, where events in the application may be logged as they happen.  The next round of questions deals with how we access the logged information.  What format is the informaiton in?  How is this "tracker hierarchy" exposed to dashboards?  How do we obtain just one tracker's worth of information?

The ultimate goal of this package is to make it as quick and easy as possible to add new trackers to your code, and to be able to see the trackers in a generic UI.  An add-on project here will be to create UI's for displaying certain types of trackers, but at this point that is a secondary concern.  Get the data first, make it pretty later.  These trackers will all be physically located on a KPI machine which will house the dashboard app.  All other machines in the system will need to be able to send tracker requests to this machine, and this machine in turn will serve the current tracker states back to requesting dashboard UI's.  The requesting UI's will be remote, e.g. running in an administrator's web browser, so the data format we discuss here is for a single tracker's state in JSON format.

As an example, let's go back to the `/member/unsubscribe` tracker.  In order for the dashboard to be able to show statistics for unsubscribe activity in the system, it will need to submit a REST request to get that data.  We'll be submitting the request to a special Play module that provides tracker endpoints as a hierarchy of REST resources.  In this example, the URL will be `GET /member/unsubscribe` and the result will contain the current in-memory statistics that the Unsubscribe tracker contains, which may look like this:

```
{
  tracker: "counter",
  name: "Unsubscribes",
  desc: "Members who have unsubscribed from the system",
  hourly: {
    current: 334903,
    histogram: [
      2, 0, 0, 3, 2, 3, 0, 0, 0, 1, 0, 0, 2, 3, 6, 4, ...
    ]
  }
  daily: {
    current: 10795,
    histogram: [
      10, 2, 30, 34, 15, ...
    ]
  }
  weekly: {
    current: 1580,
    histogram: [
      10, 2, 30, 34, 15, ...
    ]
  }
} 
```

The Unsubscribe tracker is a counter, and rather than just presenting counts we are instead tracking histograms across predetermined time periods.  This should be configurable, but it's reasonable to say that we will track hourly stats for the last 24 hours, daily stats for the last week, and weekly stats for the last year, for example.  If you would like to know what the count is for the last hour, day, or week you would simply take the first element from the histogram.  If you'd like to build out a histogram you would simply take the histogram element as the data source and feed that into whatever graphing software you are using.
