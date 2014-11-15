using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;

namespace ChunwanTest
{
    public class Program
    {
        static DateTime proxyListUpdateTime = new DateTime(0L);

        public static List<WebProxy> proxyList = new List<WebProxy>();

        static void Main(string[] args)
        {
            int stopTime = 0;

            if (stopTime > 0)
            {
                Console.WriteLine("Stop in " + stopTime + " minutes.");
            }
            else
            {
                Console.WriteLine("Infinite loop");
            }

            for (int i = 0; i < ProxyList.proxies.Length; i++)
            {
                proxyList.Add(new WebProxy(ProxyList.proxies[i]));
            }
            Console.WriteLine("Built-in proxy: " + proxyList.Count);
            loadProxyList();

            DateTime saveNow = DateTime.Now;

            for (int i = 0; i < 1; i++)
            {
                new Thread(new ThreadStart(new Voter(0, 0).VoteLocal)).Start();
            }

            for (int i = 0; i < 200; i++)
            {
                Voter voter = new Voter(i, 200);
                new Thread(new ThreadStart(voter.Vote)).Start();
            }

            do
            {
                Thread.Sleep(60000);
                Voter.CollectCounts(DateTime.Now - saveNow);
                loadProxyList();
                stopTime--;
            } while (stopTime != 0);
            Environment.Exit(0);
        }

        static string[] proxy_files = {
                "proxy.txt",
                "proxy1.txt",
                "proxy2.txt",
                "proxy3.txt",
                "proxy4.txt",
                "proxy5.txt",
                "proxy6.txt",
                "proxy7.txt",
                "proxy8.txt",
                "proxy9.txt",
                "proxy10.txt",
        };

        static private List<WebProxy> loadProxyList()
        {
            List<WebProxy> newList = proxyList;
            Dictionary<string, WebProxy> proxies = new Dictionary<string, WebProxy>();
            bool reload = false;

            foreach (string filename in proxy_files)
            {
                FileInfo file = new FileInfo(filename);

                try
                {
                    if (file.Exists && 
                            (file.LastWriteTime > proxyListUpdateTime || file.CreationTime > proxyListUpdateTime))
                    {
                        reload = true;
                    }
                }
                catch (Exception e)
                {
                    Console.WriteLine("Warn: cannot access " + filename);
                }
            }

            if (reload) {
                foreach (string filename in proxy_files)
                {
                    FileInfo file = new FileInfo(filename);

                    try
                    {
                        if (file.Exists)
                        {
                            System.IO.StreamReader reader = new System.IO.StreamReader(file.FullName);
                            string line = null;
                            while ((line = reader.ReadLine()) != null)
                            {
                                line = line.Trim();
                                if (!proxies.ContainsKey(line))
                                {
                                    try
                                    {
                                        WebProxy proxy = new WebProxy(line);
                                        proxies[line] = proxy;
                                    }
                                    catch (Exception e)
                                    {
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Console.WriteLine("Warn: cannot access " + filename);
                    }
                }
                proxyListUpdateTime = DateTime.Now;
                proxyList = new List<WebProxy>(proxies.Values);
                Console.WriteLine("Proxy list updated: " + proxyList.Count);
            }
            return proxyList;
        }
    }

    public class Voter
    {
        public Voter(int i, int total)
        {
            this.index = 1;
            this.total = total;
        }
        public static Int64 totalSuccess = 0;
        public static Int64 totalFailure = 0;
        public static Int64 totalException = 0;

        public static Int64 successCount = 0;
        public static Int64 failureCount = 0;
        public static Int64 exceptionCount = 0;

        public static List<int> messagesToPost = new List<int>();

        static string url = "http://app2.vote.cntv.cn/makeVoteAction.do";

        static string referer = "http://chunwan.cntv.cn/2014/tp/index.shtml";
        static string host = "app2.vote.cntv.cn";
        static string agent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0";

        static Random rnd = new Random();
        private int index;
        private int total;

        public void VoteLocal()
        {
            do
            {
                try
                {
                    string responseFromServer = SendRequest(null);
                    if (responseFromServer.IndexOf("successfully") >= 0)
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
                    Interlocked.Increment(ref exceptionCount);
                }
                Thread.Sleep(180000);
            } while (true);

        }

        public void Vote()
        {
            do
            {
                try
                {
                    WebProxy proxy = grabNewProxy();

                    if (proxy == null)
                    {
                        Thread.Sleep(60000);
                    }
                    else
                    {
                        proxy.BypassProxyOnLocal = true;

                        string responseFromServer = SendRequest(proxy);
                        if (responseFromServer.IndexOf("successfully") >= 0)
                        {
                            Interlocked.Increment(ref successCount);
                        }
                        else
                        {
                            Interlocked.Increment(ref failureCount);
                        }
                    }
                }
                catch (Exception e)
                {
                    Interlocked.Increment(ref exceptionCount);
                }
            } while (true);

        }

        private static string SendRequest(WebProxy proxy)
        {
            var postData = "encoding=gb2312&_charset_=UTF-8&" +
                    "retUrl=http%3A%2F%2Fapp1.vote.cntv.cn%2FviewResult.jsp%3FvoteId%3D12384&" +
                    "formhash=69256b23&voteId=12384&type=html&items_1118821=131966&submitBtn=%E6%8A%95%E7%A5%A8";
            var data = Encoding.ASCII.GetBytes(postData);

            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Referer = referer;
            request.UserAgent = agent;
            //                    request.Headers.Add("Host:"+host);

            // Set the Method property of the request to POST.
            request.Method = "POST";

            if (proxy != null)
            {
                request.Proxy = proxy;
            }

            request.ContentType = "application/x-www-form-urlencoded";
            request.ContentLength = data.Length;

            using (var stream = request.GetRequestStream())
            {
                stream.Write(data, 0, data.Length);
            }

            // Get the response.
            WebResponse response = request.GetResponse();
            // Display the status.
            // Get the stream containing content returned by the server.
            Stream dataStream = response.GetResponseStream();
            // Open the stream using a StreamReader for easy access.
            StreamReader reader = new StreamReader(dataStream);
            // Read the content.
            string responseFromServer = reader.ReadToEnd();
            try
            {
                // Clean up the streams.
                reader.Close();
                dataStream.Close();
                response.Close();
            }
            catch (Exception e)
            {
            }
            return responseFromServer;
        }

        internal static void CollectCounts(TimeSpan timespan)
        {
            totalSuccess += successCount;
            totalFailure += failureCount;
            totalException += exceptionCount;
                 
            Console.WriteLine("Voted=" + totalSuccess + "/" + successCount +
                    " (" + timespan + ")" + " Rejected=" + totalFailure + "/" + failureCount +
                    " Exception=" + totalException + "/" + exceptionCount);
            successCount = 0;
            failureCount = 0;
            exceptionCount = 0;
        }

        private static WebProxy grabNewProxy()
        {
            List<WebProxy> proxies = Program.proxyList;
            if (proxies.Count > 0)
            {
                WebProxy proxy = proxies[rnd.Next(0, proxies.Count)];
                return proxy;
            }
            else
            {
                return null;
            }
        }

        private static WebProxy grabNewProxy(int i)
        {
            List<WebProxy> proxies = Program.proxyList;
            if (proxies.Count > i)
            {
                WebProxy proxy = proxies[i];
                return proxy;
            }
            else
            {
                return null;
            }
        }
    }

    public class ProxyList
    {
        static public string[] proxies = {
        };
    }
}
