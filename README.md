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
