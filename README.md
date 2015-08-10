# kpi
A library that uses Akka-based actors to gather and report KPI in real time to your admin app.

## Purpose

While contemplating creating an administrative dashboard for the Pirc back-end platform, I was thinking about what data I was going to show and trying to figure out how I would show it.  I was also wondering whether there were any third party applications that I would like to use to do my dashboarding.  The problem that I ran into was that my dashboard/admin app is both a place where other Pirc stakeholders would come to for current statistics and KPI's, and it was a place that I would want to go to be able to not only see the curent state of key system processes but also a place where I would want to perhaps kick off or otherwise manage other processes in the system.

Consider the following examples of things I need my admin app to do:

* **Counts:** how many subscribers do I have?  How many people have cancelled their subscription?  How many in the last hour, the last day, the last week?
