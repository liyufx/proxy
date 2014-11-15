using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;

namespace ChunwanNoLimit
{
    class ChunwanNoLimit
    {
        static int stopTime = -1;

        static void Main(string[] args)
        {
            if (stopTime > 0)
            {
                Console.WriteLine("Stop in " + stopTime + " minutes.");
            }
            else
            {
                Console.WriteLine("Infinite loop - vote program");
            }

            DateTime saveNow = DateTime.Now;

            for (int i = 0; i < 100; i++)
            {
                Voter voter = new Voter();
                new Thread(new ThreadStart(voter.Vote)).Start();
            }

            do
            {
                Thread.Sleep(60000);
                Voter.CollectCounts(DateTime.Now - saveNow);
                stopTime--;
            } while (stopTime != 0);
            Environment.Exit(0);
        }

        class Voter
        {
            public static Int64 totalSuccess = 0;
            public static Int64 totalFailure = 0;

            public static Int64 successCount = 0;
            public static Int64 failureCount = 0;

            public void Vote()
            {
                do
                {
                    try
                    {
                        string URLVote = "http://apiserver.cctvweishi.com/cctv/page/vote";

                        using (WebClient webClient = new WebClient())
                        {
                            webClient.Headers["User-Agent"] = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0";
                            webClient.Headers["Accept"] = "application/json, text/javascript, */*; q=0.01";
                            webClient.Headers["Accept-Language"] = "en-US,en;q=0.5";
                            webClient.Headers["Accept-Encoding"] = "gzip, deflate";
                            //            webClient.Headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8";
                            webClient.Headers["X-Requested-With"] = "XMLHttpRequest";
                            webClient.Headers["Referer"] = "http://apiserver.cctvweishi.com/cctv/page/votedet?thread_id=6574&forum_id=189";

                            NameValueCollection formData = new NameValueCollection();
                            formData["forum_id"] = "189";
                            formData["opt_id"] = "4689";
                            formData["thread_id"] = "6574";

                            for (int i = 0; i < 2; i++)
                            {
                                byte[] responseBytes = webClient.UploadValues(URLVote, "POST", formData);
                                string result = Encoding.UTF8.GetString(responseBytes);

                                Interlocked.Increment(ref successCount);
                            }
                        }

                    }
                    catch (Exception e)
                    {
                        Interlocked.Increment(ref failureCount);

                        Console.Error.WriteLine(e);
                    }
                } while (true);
            }

            internal static void CollectCounts(TimeSpan timespan)
            {
                totalSuccess += successCount;
                totalFailure += failureCount;
                Console.WriteLine("Program Voted =" + totalSuccess + "/" + successCount +
                        " (" + timespan + ")" + " Failure=" + totalFailure + "/" + failureCount);
                successCount = 0;
                failureCount = 0;
            }
        }

        class Voter1
        {

            static string referer = "http://cctv.cntv.cn/special/2014chunwandiaocha/index.shtml";
            static string host = "app2.vote.cntv.cn";

            static string url_base = "http://app2.vote.cntv.cn/makeJSVoteAction.do?voteId=12587&items_1118825=132081&time={time}&type=json";
//            static string url_base = "http://app2.vote.cntv.cn/makeJSVoteAction.do?voteId=12583&items_1118820=132038&time={time}&type=json";

            public static Int64 totalSuccess = 0;
            public static Int64 totalFailure = 0;

            public static Int64 successCount = 0;
            public static Int64 failureCount = 0;

            public void Vote()
            {
                do
                {
                    if (successCount > 200)
                    {
                        Thread.Sleep(2000);
                    }
                    else
                    {
                        try
                        {

                            string url = url_base.Replace("{time}", CurrentTimeMillis().ToString());

                            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
                            request.Referer = referer;
                            request.UserAgent = "Mozilla/5.0";
                            request.Timeout = 10000;
                            // Set the Method property of the request to POST.
                            request.Method = "GET";

                            // Get the response.
                            WebResponse response = request.GetResponse();
                            // Display the status.
                            // Get the stream containing content returned by the server.
                            Stream dataStream = response.GetResponseStream();
                            // Open the stream using a StreamReader for easy access.
                            StreamReader reader = new StreamReader(dataStream);
                            // Read the content.
                            string responseFromServer = reader.ReadToEnd();

                            if (responseFromServer.IndexOf("\'success\':'1'") >= 0)
                            {
                                Interlocked.Increment(ref successCount);
                            }
                            else
                            {
                                Interlocked.Increment(ref failureCount);
                            }
                        }
                        catch (Exception e)
                        {
                            Interlocked.Increment(ref failureCount);
                        }
                    }
                } while (ChunwanNoLimit.stopTime != 0);
            }

            internal static void CollectCounts(TimeSpan timespan)
            {
                totalSuccess += successCount;
                totalFailure += failureCount;
                Console.WriteLine("Program Voted (limit 200/m)=" + totalSuccess + "/" + successCount +
                        " (" + timespan + ")" + " Failure=" + totalFailure + "/" + failureCount);
                successCount = 0;
                failureCount = 0;
            }
        }

        private static readonly DateTime Jan1st1970 = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static long CurrentTimeMillis()
        {
            return (long)(DateTime.UtcNow - Jan1st1970).TotalMilliseconds;
        }
    }
}
