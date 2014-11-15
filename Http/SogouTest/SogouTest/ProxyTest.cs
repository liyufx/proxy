using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;

namespace ProxyTest
{
/*
    class ProxyTest
    {
        static DateTime proxyListUpdateTime = new DateTime(0L);

        public static List<TimedProxy> proxyList = new List<TimedProxy>();
        public static Dictionary<string, TimedProxy> liveProxies = new Dictionary<string, TimedProxy>();
        public static Dictionary<string, TimedProxy> exceptionProxies = new Dictionary<string, TimedProxy>();
        public static string site = System.Configuration.ConfigurationManager.AppSettings["site"];
        public static int rspLength;
        public static int timeout;

        static void MainTest(string[] args)
        {
            try
            {
                timeout = int.Parse(System.Configuration.ConfigurationManager.AppSettings["rspTimeout"]);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("rspTimeout mis-configured, default to 3 seconds: " + e);
                timeout = 3000;
            }

            try
            {
                rspLength = int.Parse(System.Configuration.ConfigurationManager.AppSettings["rspLength"]);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("rspLength mis-configured, default to 2000 characters: " + e);
                rspLength = 2000;
            }

            int stopTime;

            try
            {
                stopTime = int.Parse(System.Configuration.ConfigurationManager.AppSettings["stopTimeMinutes"]);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("stopTimeMinutes mis-configured, default to infinite loop: " + e);
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

            int threadsCount;
            try
            {
                threadsCount = int.Parse(System.Configuration.ConfigurationManager.AppSettings["threadsCount"]);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("threadsCount mis-configured, default to 100: " + e);
                threadsCount = 100;
            }
            Console.WriteLine("Thread: " + threadsCount);


            Dictionary<string, TimedProxy> proxies = new Dictionary<string, TimedProxy>();
            for (int i = 0; i < ProxyList.proxies.Length; i++)
            {
                if (!proxies.ContainsKey(ProxyList.proxies[i]))
                {
                    try
                    {
                        TimedProxy proxy = new TimedProxy(ProxyList.proxies[i]);
                        proxies[ProxyList.proxies[i]] = proxy;
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
            proxyList = new List<TimedProxy>(proxies.Values); 

            Console.WriteLine("Built-in proxy: " + proxyList.Count);
            loadProxyList();

            DateTime saveNow = DateTime.Now;

            
                        for (int i = 0; i < 5; i++)
                        {
                            new Thread(new ThreadStart(new Voter(0, 0).VoteLocal)).Start();
                        }
             
            for (int i = 0; i < threadsCount; i++)
            {
                Voter voter = new Voter(i, 200);
                new Thread(new ThreadStart(voter.Vote)).Start();
            }
            do
            {
                Thread.Sleep(60000);
                TimeSpan timePast = DateTime.Now - saveNow;
                Voter.CollectCounts( timePast);
                loadProxyList();
                if ((int)(timePast.TotalMinutes) % 10 == 0) 
                {
                    dumpLiveProxies();
                }
                stopTime--;
            } while (stopTime != 0);
            Environment.Exit(0);
        }

        private static void dumpLiveProxies()
        {
            string filename = site+"-liveproxies.txt";
            try
            {
                using (System.IO.StreamWriter writer = new System.IO.StreamWriter(filename))
                {
                    foreach (string key in liveProxies.Keys)
                    {
                        writer.WriteLine("\"" + key + "\",");
                    }
                }
            }
            catch (Exception e)
            {
                Console.WriteLine("Cannot write to " + filename + ": " + e.Message);
            }
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

        static private List<TimedProxy> loadProxyList()
        {
            List<TimedProxy> newList = proxyList;
            Dictionary<string, TimedProxy> proxies = new Dictionary<string, TimedProxy>();
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

            if (reload)
            {
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
                                        TimedProxy proxy = new TimedProxy(line);
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
                foreach (var entry in ProxyTest.liveProxies) {
                    proxies[entry.Key] = entry.Value;
                }

                foreach (var entry in ProxyTest.exceptionProxies) {
                    proxies[entry.Key] = entry.Value;
                }

                proxyListUpdateTime = DateTime.Now;
                proxyList = new List<TimedProxy>(proxies.Values);
                exceptionProxies = new Dictionary<string,TimedProxy>();
                Console.WriteLine("Proxy list updated: " + proxyList.Count);
            }
            return proxyList;
        }
    }

    public class Voter1
    {

        public Voter1(int i, int total)
        {
            this.index = 1;
            this.total = total;
            searchUrl = System.Configuration.ConfigurationManager.AppSettings["url"];
            referer = System.Configuration.ConfigurationManager.AppSettings["referer"];

            if (string.IsNullOrEmpty(referer)) referer = searchUrl;

            rspMarker = System.Configuration.ConfigurationManager.AppSettings["rspMarker"];
        }
        
        public static Int64 totalSuccess = 0;
        public static Int64 totalFailure = 0;
        public static Int64 totalException = 0;
        public static Int64 totalSearch= 0;

        public static Int64 successCount = 0;
        public static Int64 searchCount = 0;
        public static Int64 failureCount = 0;
        public static Int64 exceptionCount = 0;

        public static List<int> messagesToPost = new List<int>();

        private string searchUrl;
        private string rspMarker;
        private string referer;

        static string agent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:26.0) Gecko/20100101 Firefox/26.0";

        static Random rnd = new Random();
        private int index;
        private int total;

        public void VoteLocal()
        {
            for (int i=0; i<1000; i++)
            {
                try
                {
                    string voteZuoUrl = "http://123.sogou.com/api/popularking/vote.php?action=incr&id=3&ename=zuoli&name=%E5%B7%A6%E7%AB%8B&type=1&uid=3&r=d8073469adf7582804&s=3f4e441c5897b5&x=12_5_4_3_2_0_1_9_11_8_6_7_10,51,1681403959,859059769,1633969719,892875320,1597202227,808746033,964441442,897333560,926496821,1697932851,1664169012,878653494&y=d0d2422cae0901c46f75ea24af6d8a22704bc0f4ff43dc5c130ed93681a0129720538ebebb82988202a6b4c84438ff05e7aba6f6c16133330ab998613c8aeac7&ccc=1392418013755";
//                    voteZuoUrl = voteZuoUrl.Replace("{time}", CurrentTimeMillis().ToString());

                    string responseFromServer = SendRequest(voteZuoUrl, null);
                    if (responseFromServer.IndexOf("\"code\":0,\"msg\":\"ok\"") >= 0)
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
                Thread.Sleep(1000);
            }

        }

        public void Vote()
        {
            do
            {
                using (LockedTimedProxy lockedProxy = grabNewProxy()) 
                {
                    TimedProxy proxy = lockedProxy.Proxy;

                    try
                    {
                        if (proxy.readyToTry())
                        {
                            if (proxy.readyToVote())
                            {
                                string voteUrl = "http://123.sogou.com/api/popularking/vote.php?action=incr&id=6&ename=yaobeina&name=%E5%A7%9A%E8%B4%9D%E5%A8%9C&type=2&uid=6&ccc={time}";
                                voteUrl = voteUrl.Replace("{time}", CurrentTimeMillis().ToString());

                                string responseFromServer = SendRequest(voteUrl, proxy);
                                if (responseFromServer.IndexOf("\"code\":0,\"msg\":\"ok\"") >= 0)
                                {
                                    Interlocked.Increment(ref successCount);
                                }
                                else
                                {
                                    Interlocked.Increment(ref failureCount);
                                }
                            }

                            string response = SendRequest(searchUrl, proxy);
                            if (response.Length > ProxyTest.rspLength && response.Contains(rspMarker))
                            {
                                Interlocked.Increment(ref searchCount);
                                proxy.recordSuccess();
                                ProxyTest.liveProxies[proxy.Key] = proxy;
                                ProxyTest.exceptionProxies.Remove(proxy.Key);
                            }
                            else
                            {
                                throw new Exception("Unexpected short response");
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Interlocked.Increment(ref exceptionCount);
                        proxy.recordSuccess();
                        ProxyTest.liveProxies.Remove(proxy.Key);
                        ProxyTest.exceptionProxies[proxy.Key] = proxy;
                    }
                }
            } while (true);

        }

        private string SendRequest(string url, WebProxy proxy)
        {
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Referer = referer;
            request.UserAgent = "Mozilla/5.0";
            request.Timeout = ProxyTest.timeout;
            // Set the Method property of the request to POST.
            request.Method = "GET";

            if (proxy != null)
            {
                request.Proxy = proxy;
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
            totalSearch += searchCount;

            //Console.WriteLine("Voted=" + totalSuccess + "/" + successCount +
            //        " (" + timespan + ")" + " Rejected=" + totalFailure + "/" + failureCount +
            //        " Exception=" + totalException + "/" + exceptionCount + " Live proxies=" + 
            //        SogouProxy.liveProxies.Count + " exception proxies=" + SogouProxy.exceptionProxies.Count);
            Console.WriteLine("Search=" + totalSearch + "/" + searchCount +
                    " (" + timespan + ")" +
                    " Exception=" + totalException + "/" + exceptionCount + " (Proxies Live=" + 
                    ProxyTest.liveProxies.Count + " exception=" + ProxyTest.exceptionProxies.Count + ")");
            searchCount = 0;
            successCount = 0;
            failureCount = 0;
            exceptionCount = 0;
        }

        private static LockedTimedProxy grabNewProxy()
        {
            do {
                List<TimedProxy> proxies = ProxyTest.proxyList;
                if (proxies.Count > 0)
                {
                    for (int i=0; i<20; i++) {
                        try {
                            TimedProxy proxy = proxies[rnd.Next(0, proxies.Count)];
                            if (proxy.readyToTry()) {
                                return new LockedTimedProxy(proxy);
                            }
                        } catch (Exception e) {
                            // cannot lock, try another one
                        }
                    }
                }
                // cannot get a ready proxy, sleep 30 seconds before retry
                Thread.Sleep(30000);
            } while (true);
        }

        private static TimedProxy grabNewProxy(int i)
        {
            List<TimedProxy> proxies = ProxyTest.proxyList;
            if (proxies.Count > i)
            {
                TimedProxy proxy = proxies[i];
                return proxy;
            }
            else
            {
                return null;
            }
        }

        private static readonly DateTime Jan1st1970 = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static long CurrentTimeMillis()
        {
            return (long)(DateTime.UtcNow - Jan1st1970).TotalMilliseconds;
        }
    }

    public class LockedTimedProxy1 : IDisposable
    {
        TimedProxy proxy;

        public LockedTimedProxy1(TimedProxy proxy) {
            this.proxy = proxy;
            proxy.tryLock();
        }

        public TimedProxy Proxy { get {return proxy;} }

        public void Dispose()
        {
            proxy.unlock();
        }
    }

    public class TimedProxy1 : WebProxy
    {
        string key;
        DateTime ? successTime = null;
        DateTime ? rejectionTime = null;
        DateTime ? exceptionTime = null;
        int exceptionCount = 0;
        private long timeInterval;
        private Object mLock = new Object();

        public TimedProxy1(string proxy) : base(proxy)
        {
            key = proxy;
            BypassProxyOnLocal = true;

            try
            {
                timeInterval = int.Parse(System.Configuration.ConfigurationManager.AppSettings["retryInterval"]);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("rspLength mis-configured, default to 2000 characters: " + e);
                timeInterval = 3*3600;
            }

        }

        public bool readyToTry()
        {
            if (exceptionTime == null || (DateTime.Now - ExceptionTime).TotalSeconds > 7200 ||
                (DateTime.Now - ExceptionTime).TotalSeconds > 300 * (2 ^ exceptionCount))
            {
                return (successTime == null || (DateTime.Now - SuccessTime).TotalSeconds > timeInterval) &&
                 (rejectionTime == null || (DateTime.Now - RejectionTime).TotalSeconds > timeInterval / 3);
            }
            else
            {
                return false;
            }
        }

        public string Key { get { return key; } }

        public bool readyToVote()
        {
            //return (successTime==null || (DateTime.Now - SuccessTime).TotalSeconds >timeInterval) &&
            //     (rejectionTime==null || (DateTime.Now - RejectionTime).TotalSeconds > timeInterval/10);
            return false;
        }

        DateTime ExceptionTime
        {
            get { return exceptionTime ?? DateTime.MinValue; }
        }

        DateTime SuccessTime
        {
            get { return successTime ?? DateTime.MinValue; }
        }

        DateTime RejectionTime
        {
            get { return rejectionTime ?? DateTime.MinValue; }
        }

        public void recordSuccess()
        {
            successTime = DateTime.Now;
            rejectionTime = null;
            exceptionTime = null;
            exceptionCount = 0;
        }

        public void recordRejection()
        {
            rejectionTime = DateTime.Now;
            exceptionTime = null;
            exceptionCount = 0;
        }

        public void recordException()
        {
            exceptionTime = DateTime.Now;
            exceptionCount++;
        }

        internal void tryLock()
        {
            if (!Monitor.TryEnter(mLock))
            {
                throw new Exception("Cannot lock " + this);
            }
        }

        internal void unlock()
        {
            Monitor.Exit(mLock);
        }
    }

    public class ProxyList1
    {
        static public string[] proxies = {
        };
    }
*/
}
