package org.change.v2.helpers;

import java.util.ArrayList;
import java.util.List;

public class GFG
{
    public static class EmbeddedResult {
        public int bpm;
        public Iterable<Integer> notMatched;
    }
    // N is cardinality of the
    // sets currently being matched
    int N;

    public GFG(int N) {
        this.N = N;
    }

    // A DFS based recursive function that
    // returns true if a matching for
    // vertex u is possible
    boolean bpm(Iterable<Integer>[] bpGraph, int u,
                boolean seen[], int matchR[])
    {
        // Try every job one by one
        for (int v : bpGraph[u])
        {
            // If applicant u is interested
            // in job v and v is not visited
            if (!seen[v])
            {
                // Mark v as visited
                seen[v] = true;
                // If job 'v' is not assigned to
                // an applicant OR previously
                // assigned applicant for job v (which
                // is matchR[v]) has an alternate job available.
                // Since v is marked as visited in the
                // above line, matchR[v] in the following
                // recursive call will not get job 'v' again
                if (matchR[v] < 0 || bpm(bpGraph, matchR[v],
                        seen, matchR))
                {
                    matchR[v] = u;
                    return true;
                }
            }
        }
        return false;
    }

    // Returns maximum number
    // of matching from M to N
    public int maxBPM(Iterable<Integer>[] bpGraph)
    {
        // An array to keep track of the
        // applicants assigned to jobs.
        // The value of matchR[i] is the
        // applicant number assigned to job i,
        // the value -1 indicates nobody is assigned.
        int matchR[] = new int[N];

        // Initially all jobs are available
        for(int i = 0; i < N; ++i)
            matchR[i] = -1;

        // Count of jobs assigned to applicants
        int result = 0;
        int u = 0;
        for (; u < bpGraph.length; u++)
        {
            // Mark all jobs as not seen
            // for next applicant.
            boolean seen[] =new boolean[N] ;
            for(int i = 0; i < N; ++i)
                seen[i] = false;

            // Find if the applicant 'u' can get a job
            if (bpm(bpGraph, u, seen, matchR))
                result++;
        }
        return result;
    }
    public EmbeddedResult maxBPMWithExplanation(Iterable<Integer>[] bpGraph)
    {
        // An array to keep track of the
        // applicants assigned to jobs.
        // The value of matchR[i] is the
        // applicant number assigned to job i,
        // the value -1 indicates nobody is assigned.
        int matchR[] = new int[N];

        // Initially all jobs are available
        for(int i = 0; i < N; ++i)
            matchR[i] = -1;

        // Count of jobs assigned to applicants
        int result = 0;
        int u = 0;
        EmbeddedResult res = new EmbeddedResult();
        List<Integer> nonMatched = new ArrayList<>();
        for (; u < bpGraph.length; u++)
        {
            // Mark all jobs as not seen
            // for next applicant.
            boolean seen[] =new boolean[N] ;
            for(int i = 0; i < N; ++i)
                seen[i] = false;

            // Find if the applicant 'u' can get a job
            if (bpm(bpGraph, u, seen, matchR))
                result++;
            else
                nonMatched.add(u);
        }
        if (!nonMatched.isEmpty())
            res.notMatched = nonMatched;
        res.bpm = result;
        return res;
    }
}