using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Net;
using System.Text;
using System.Threading;


namespace QqVoiceVote
{
    class QqVoiceVote
    {

        static void Main(string[] args)
        {
            int stopTime;

            try
            {
                stopTime = int.Parse(System.Configuration.ConfigurationManager.AppSettings["stopTimeMinutes"]);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("stopTimeMinutes mis-configured, default to infinite loop.");
                stopTime = 0;
            }

            if (stopTime > 0)
            {
                Console.WriteLine("Stop in " + stopTime + " minutes.");
            }
            else
            {
                Console.WriteLine("Infinite loop");
            }

            DateTime saveNow = DateTime.Now;

            int threadsCount = 20;

            try
            {
                threadsCount = int.Parse(System.Configuration.ConfigurationManager.AppSettings["threadsCount"]);
                Console.Out.WriteLine("threadsCount = " + threadsCount);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("threadsCount mis-configured, default to " + threadsCount);
            }

            for (int i = 0; i < threadsCount; i++)
            {
                Voter voter = new Voter(1000000);
                new Thread(new ThreadStart(voter.Vote)).Start();
            }
            do
            {
                Thread.Sleep(60000);
                TimeSpan timePast = DateTime.Now - saveNow;
                Voter.CollectCounts( timePast);
                stopTime--;
            } while (stopTime != 0);
            Environment.Exit(0);
        }
    }

    class Voter
    {
        public static Int64 totalSuccess = 0;
        public static Int64 totalFailure = 0;
        public static Int64 totalException = 0;

        public static Int64 successIPCount = 0;
        public static Int64 successCount = 0;
        public static Int64 failureCount = 0;
        public static Int64 exceptionCount = 0;
        private int iteration;

        public Voter(int i)
        {
            this.iteration = i;
        }

        public void Vote()
        {
            for (int j = 0; j < iteration; j++)
            {
                try
                {
                    string URLVote = "http://panshi.qq.com/vote/10003523/submit";

                    using (WebClient webClient = new WebClient())
                    {
                        //            webClient.Headers["Host"] = "minisite.youku.com";
                        webClient.Headers["User-Agent"] = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0";
                        webClient.Headers["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
                        webClient.Headers["Accept-Language"] = "en-US,en;q=0.5";
                        webClient.Headers["Accept-Encoding"] = "gzip, deflate";
                        //            webClient.Headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8";
                        webClient.Headers["X-Requested-With"] = "XMLHttpRequest";
                        webClient.Headers["Referer"] = "http://v.qq.com/voice/";

                        NameValueCollection formData = new NameValueCollection();
                        formData["answer"] = "{\"6959\":{\"selected\":[\"25718\"]},\"6960\":{\"selected\":[\"25721\"]},\"6961\":{\"selected\":[\"25727\"]},\"6962\":{\"selected\":[\"25730\"]}}";
                        formData["login"] = "1";
                        formData["source"] = "1";
                        formData["format"] = "script";
                        formData["callback"] = "parent.callback";

                        byte[] responseBytes = webClient.UploadValues(URLVote, "POST", formData);
                        string result = Encoding.UTF8.GetString(responseBytes);

                        if (result.Length>0)
                        {
                            Interlocked.Increment(ref successCount);
                        }
                        else
                        {
                            Interlocked.Increment(ref failureCount);
                            //                            Console.Error.WriteLine("Cannot vote: " + result);
                        }
                        Thread.Sleep(3000);
                    }
                }
                catch (Exception e)
                {
                    Interlocked.Increment(ref exceptionCount);

                    Console.Error.WriteLine(e);
                }
            }
        }


        internal static void CollectCounts(TimeSpan timespan)
        {
            totalSuccess += successCount;
            totalFailure += failureCount;
            totalException += exceptionCount;

            Console.WriteLine("Voted=" + totalSuccess + "/" + successCount + 
                    " (" + timespan + ")" + " Failed=" + totalFailure + "/" + failureCount +
                    " Exception=" + totalException + "/" + exceptionCount);
            successIPCount = 0;
            successCount = 0;
            failureCount = 0;
            exceptionCount = 0;
        }

    }
}
