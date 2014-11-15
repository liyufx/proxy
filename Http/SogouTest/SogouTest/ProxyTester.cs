using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;

namespace ProxyTest
{
    class ProxyTester
    {
        static DateTime proxyListUpdateTime = new DateTime(0L);

        public static List<TimedProxy> proxyList = new List<TimedProxy>();
        public static Dictionary<string, TimedProxy> liveProxies = new Dictionary<string, TimedProxy>();
        public static Dictionary<string, TimedProxy> exceptionProxies = new Dictionary<string, TimedProxy>();
        public static string site = System.Configuration.ConfigurationManager.AppSettings["site"];
        public static int rspLength;
        public static int timeout;

        static void Main(string[] args)
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
            loadProxyList(true);

            DateTime saveNow = DateTime.Now;

            for (int i = 0; i < threadsCount; i++)
            {
                Voter voter = new Voter(i, 200);
                new Thread(new ThreadStart(voter.Vote)).Start();
            }
            int lastProxyLoadTime = 0;
            bool uploaded = false;
            do
            {
                Thread.Sleep(60000);
                TimeSpan timePast = DateTime.Now - saveNow;
                Voter.CollectCounts( timePast);
                lastProxyLoadTime++;
                if (loadProxyList(lastProxyLoadTime > 10))
                {
                    lastProxyLoadTime = 0;
                }
                if ((!uploaded && (int)(timePast.TotalMinutes)>20 ) || ((!uploaded || (int)(timePast.TotalMinutes) % 10 == 0) &&
                       (uploaded || (liveProxies.Count + exceptionProxies.Count) * 2 > proxyList.Count)) )
                {
                    if (!uploadLiveProxies())
                    {
                        dumpLiveProxies();
                    }
                    else
                    {
                        uploaded = true;
                        Console.Out.WriteLine("Successfully uploaded " + liveProxies.Count +
                                " live proxies to " +
                                System.Configuration.ConfigurationManager.AppSettings["serverURL"]);
                    }
                }
                stopTime--;
            } while (stopTime != 0);
            Environment.Exit(0);
        }

        private static bool uploadLiveProxies()
        {
            try
            {
                using (var client = new WebClient())
                {
                    using (StringWriter writer = new StringWriter())
                    {
                        List<string> keys = new List<String>(proxyList.Count);
                        keys.AddRange(liveProxies.Keys);
                        foreach (string key in keys)
                        {
                            writer.WriteLine(key);
                        }
                        client.Headers[HttpRequestHeader.ContentType] = "text/plain";
                        var response = client.UploadString(
                                System.Configuration.ConfigurationManager.AppSettings["serverURL"] + 
                                        "?site=" + System.Configuration.ConfigurationManager.AppSettings["site"] +
                                        "&zone=" + System.Configuration.ConfigurationManager.AppSettings["zone"], 
                                writer.ToString());

                        return string.Compare("success", response.Trim(), true)==0;
                    }
                }
            }
            catch (Exception e)
            {
                Console.Error.WriteLine("Cannot upload proxy information to " + 
                        System.Configuration.ConfigurationManager.AppSettings["serverURL"] +
                        ", writing to " + site +"-liveproxies.txt to backup.");
            }
            return false;
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

        static private bool loadProxyList(bool forceReload)
        {
            List<TimedProxy> newList = proxyList;
            Dictionary<string, TimedProxy> proxies = new Dictionary<string, TimedProxy>();
            bool reload = forceReload;

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
                            using (StreamReader reader = new System.IO.StreamReader(file.FullName))
                            {
                                loadProxyFromReader(proxies, reader);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Console.WriteLine("Warn: cannot access " + filename);
                    }
                }

                loadProxyFromUrl(System.Configuration.ConfigurationManager.AppSettings["proxyURL"], proxies);

                List<TimedProxy> liveProxies = new List<TimedProxy>(ProxyTester.liveProxies.Values);
                foreach (var proxy in liveProxies)
                {
                    if (proxy != null)
                    {
                        proxies[proxy.Key] = proxy;
                    }
                }

                List<TimedProxy> exceptionPx = new List<TimedProxy>(ProxyTester.exceptionProxies.Values);
                foreach (var proxy in exceptionPx)
                {
                    if (proxy != null)
                    {
                        proxies[proxy.Key] = proxy;
                    }
                }

                proxyListUpdateTime = DateTime.Now;
                proxyList = new List<TimedProxy>(proxies.Values);
                ProxyTester.exceptionProxies = new Dictionary<string, TimedProxy>();
                Console.WriteLine("Proxy list updated: " + proxyList.Count);
                return true;
            }
            return false;
        }

        private static void loadProxyFromReader(Dictionary<string, TimedProxy> proxies, System.IO.StreamReader reader)
        {
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

        private static void loadProxyFromUrl(string url, Dictionary<string, TimedProxy> proxies)
        {
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.UserAgent = "Mozilla/5.0";
            request.Timeout = 120000;
            // Set the Method property of the request to POST.
            request.Method = "GET";

            // Get the response.
            using (WebResponse response = request.GetResponse()) 
                using (Stream dataStream = response.GetResponseStream())
                    using (StreamReader reader = new StreamReader(dataStream)) {
                        loadProxyFromReader(proxies, reader);
            }
        }
    }

    public class Voter
    {

        public Voter(int i, int total)
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
        public static Int64 totalSearch = 0;

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
                            string response = SendRequest(searchUrl, proxy);
                            if (response.Length > ProxyTester.rspLength && response.Contains(rspMarker))
                            {
                                Interlocked.Increment(ref searchCount);
                                proxy.recordSuccess();
                                ProxyTester.liveProxies[proxy.Key] = proxy;
                                ProxyTester.exceptionProxies.Remove(proxy.Key);
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
                        ProxyTester.liveProxies.Remove(proxy.Key);
                        ProxyTester.exceptionProxies[proxy.Key] = proxy;
                    }
                }
            } while (true);

        }

        private string SendRequest(string url, WebProxy proxy)
        {
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Referer = referer;
            request.UserAgent = "Mozilla/5.0";
            request.Timeout = ProxyTester.timeout;
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
                    ProxyTester.liveProxies.Count + " exception=" + ProxyTester.exceptionProxies.Count + ")");
            searchCount = 0;
            successCount = 0;
            failureCount = 0;
            exceptionCount = 0;
        }

        private static LockedTimedProxy grabNewProxy()
        {
            do
            {
                List<TimedProxy> proxies = ProxyTester.proxyList;
                if (proxies.Count > 0)
                {
                    for (int i = 0; i < 20; i++)
                    {
                        try
                        {
                            TimedProxy proxy = proxies[rnd.Next(0, proxies.Count)];
                            if (proxy.readyToTry())
                            {
                                return new LockedTimedProxy(proxy);
                            }
                        }
                        catch (Exception e)
                        {
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
            List<TimedProxy> proxies = ProxyTester.proxyList;
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

    public class LockedTimedProxy : IDisposable
    {
        TimedProxy proxy;

        public LockedTimedProxy(TimedProxy proxy)
        {
            this.proxy = proxy;
            proxy.tryLock();
        }

        public TimedProxy Proxy { get { return proxy; } }

        public void Dispose()
        {
            proxy.unlock();
        }
    }

    public class TimedProxy : WebProxy
    {
        string key;
        DateTime? successTime = null;
        DateTime? rejectionTime = null;
        DateTime? exceptionTime = null;
        int exceptionCount = 0;
        private long timeInterval;
        private Object mLock = new Object();

        public TimedProxy(string proxy)
            : base(proxy)
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
                timeInterval = 3 * 3600;
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

    public class ProxyList
    {
        static public string[] proxies = {
        };
    }
}
