using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Net;
using System.Text;
using System.Threading;

namespace ZhixuanVote
{
    class ZhixuanVote
    {

        static void Main_(string[] args)
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
            new Voter().Vote();
            TimeSpan timePast = DateTime.Now - saveNow;
            Voter.CollectCounts(timePast);
            timePast = DateTime.Now - saveNow;
            /*
                        int threadsCount = 1;


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
                            stopTime--;
                        } while (stopTime != 0);
                        Environment.Exit(0);
                    }
             */
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

        private string[] names = {
        "大赵", "大钱", "大孙", "大李", "大周", "大吴", "大郑", "大王",
        "小赵", "小钱", "小孙", "小李", "小周", "小吴", "小郑", "小王",
        "老赵", "老钱", "老孙", "老李", "老周", "老吴", "老郑", "老王",
        "大冯", "大陈", "大楮", "大卫", "大蒋", "大沈", "大韩", "大杨",
        "小冯", "小陈", "小楮", "小卫", "小蒋", "小沈", "小韩", "小杨",
        "老冯", "老陈", "老楮", "老卫", "老蒋", "老沈", "老韩", "老杨",
        "大朱", "大秦", "大尤", "大许", "大何", "大吕", "大施", "大张",
        "小朱", "小秦", "小尤", "小许", "小何", "小吕", "小施", "小张",
        "老朱", "老秦", "老尤", "老许", "老何", "老吕", "老施", "老张",
        "大孔", "大曹", "大严", "大华", "大金", "大魏", "大陶", "大姜",
        "小孔", "小曹", "小严", "小华", "小金", "小魏", "小陶", "小姜",
        "老孔", "老曹", "老严", "老华", "老金", "老魏", "老陶", "老姜",
        "大戚", "大谢", "大邹", "大喻", "大柏", "大水", "大窦", "大章",
        "小戚", "小谢", "小邹", "小喻", "小柏", "小水", "小窦", "小章",
        "老戚", "老谢", "老邹", "老喻", "老柏", "老水", "老窦", "老章",
        "大云", "大苏", "大潘", "大葛", "大奚", "大范", "大彭", "大郎",
        "小云", "小苏", "小潘", "小葛", "小奚", "小范", "小彭", "小郎",
        "老云", "老苏", "老潘", "老葛", "老奚", "老范", "老彭", "老郎",
        "大鲁", "大韦", "大昌", "大马", "大苗", "大凤", "大花", "大方",
        "小鲁", "小韦", "小昌", "小马", "小苗", "小凤", "小花", "小方",
        "老鲁", "老韦", "老昌", "老马", "老苗", "老凤", "老花", "老方",
        "大俞", "大任", "大袁", "大柳", "大酆", "大鲍", "大史", "大唐",
        "小俞", "小任", "小袁", "小柳", "小酆", "小鲍", "小史", "小唐",
        "老俞", "老任", "老袁", "老柳", "老酆", "老鲍", "老史", "老唐",
        "大费", "大廉", "大岑", "大薛", "大雷", "大贺", "大倪", "大汤",
        "小费", "小廉", "小岑", "小薛", "小雷", "小贺", "小倪", "小汤",
        "老费", "老廉", "老岑", "老薛", "老雷", "老贺", "老倪", "老汤",
        "大滕", "大殷", "大罗", "大毕", "大郝", "大邬", "大安", "大常",
        "小滕", "小殷", "小罗", "小毕", "小郝", "小邬", "小安", "小常",
        "老滕", "老殷", "老罗", "老毕", "老郝", "老邬", "老安", "老常",
        "大乐", "大于", "大时", "大傅", "大皮", "大卞", "大齐", "大康",
        "小乐", "小于", "小时", "小傅", "小皮", "小卞", "小齐", "小康",
        "老乐", "老于", "老时", "老傅", "老皮", "老卞", "老齐", "老康",
        "大伍", "大余", "大元", "大卜", "大顾", "大孟", "大平", "大黄",
        "小伍", "小余", "小元", "小卜", "小顾", "小孟", "小平", "小黄",
        "老伍", "老余", "老元", "老卜", "老顾", "老孟", "老平", "老黄",
        "大和", "大穆", "大萧", "大尹", "大姚", "大邵", "大湛", "大汪",
        "小和", "小穆", "小萧", "小尹", "小姚", "小邵", "小湛", "小汪",
        "老和", "老穆", "老萧", "老尹", "老姚", "老邵", "老湛", "老汪",
        "大祁", "大毛", "大禹", "大狄", "大米", "大贝", "大明", "大臧",
        "小祁", "小毛", "小禹", "小狄", "小米", "小贝", "小明", "小臧",
        "老祁", "老毛", "老禹", "老狄", "老米", "老贝", "老明", "老臧",
        "大计", "大伏", "大成", "大戴", "大谈", "大宋", "大茅", "大庞",
        "小计", "小伏", "小成", "小戴", "小谈", "小宋", "小茅", "小庞",
        "老计", "老伏", "老成", "老戴", "老谈", "老宋", "老茅", "老庞",
        "大熊", "大纪", "大舒", "大屈", "大项", "大祝", "大董", "大梁",
        "小熊", "小纪", "小舒", "小屈", "小项", "小祝", "小董", "小梁",
        "老熊", "老纪", "老舒", "老屈", "老项", "老祝", "老董", "老梁",
        "大杜", "大阮", "大蓝", "大闽", "大席", "大季", "大麻", "大强",
        "小杜", "小阮", "小蓝", "小闽", "小席", "小季", "小麻", "小强",
        "老杜", "老阮", "老蓝", "老闽", "老席", "老季", "老麻", "老强",
        "大贾", "大路", "大娄", "大危", "大江", "大童", "大颜", "大郭",
        "小贾", "小路", "小娄", "小危", "小江", "小童", "小颜", "小郭",
        "老贾", "老路", "老娄", "老危", "老江", "老童", "老颜", "老郭",
        "大梅", "大盛", "大林", "大刁", "大锺", "大徐", "大丘", "大骆",
        "小梅", "小盛", "小林", "小刁", "小锺", "小徐", "小丘", "小骆",
        "老梅", "老盛", "老林", "老刁", "老锺", "老徐", "老丘", "老骆",
        "大高", "大夏", "大蔡", "大田", "大樊", "大胡", "大凌", "大霍",
        "小高", "小夏", "小蔡", "小田", "小樊", "小胡", "小凌", "小霍",
        "老高", "老夏", "老蔡", "老田", "老樊", "老胡", "老凌", "老霍",
        "大虞", "大万", "大支", "大柯", "大昝", "大管", "大卢", "大莫",
        "小虞", "小万", "小支", "老柯", "小昝", "小管", "小卢", "小莫",
        "老虞", "老万", "老支", "老柯", "老昝", "老管", "老卢", "老莫",
        "大经", "大房", "大裘", "大缪", "大干", "大解", "大应", "大宗",
        "小经", "小房", "小裘", "小缪", "小干", "小解", "小应", "小宗",
        "老经", "老房", "老裘", "老缪", "老干", "老解", "老应", "老宗",
        "大丁", "大宣", "大贲", "大邓", "大郁", "大单", "大杭", "大洪",
        "小丁", "小宣", "小贲", "小邓", "小郁", "小单", "小杭", "小洪",
        "老丁", "老宣", "老贲", "老邓", "老郁", "老单", "老杭", "老洪",
        "大包", "大诸", "大左", "大石", "大崔", "大吉", "大钮", "大龚",
        "小包", "小诸", "小左", "小石", "小崔", "小吉", "小钮", "小龚",
        "老包", "老诸", "老左", "老石", "老崔", "老吉", "老钮", "老龚",
        "大程", "大嵇", "大邢", "大滑", "大裴", "大陆", "大荣", "大翁",
        "小程", "小嵇", "小邢", "小滑", "小裴", "小陆", "小荣", "小翁",
        "老程", "老嵇", "老邢", "老滑", "老裴", "老陆", "老荣", "老翁",
        "大荀", "大羊", "大於", "大惠", "大甄", "大麹", "大家", "大封",
        "小荀", "小羊", "小於", "小惠", "小甄", "小麹", "小家", "小封",
        "老荀", "老羊", "老於", "老惠", "老甄", "老麹", "老家", "老封",
        "大芮", "大羿", "大储", "大靳", "大汲", "大邴", "大糜", "大松",
        "小芮", "小羿", "小储", "小靳", "小汲", "小邴", "小糜", "小松",
        "老芮", "老羿", "老储", "老靳", "老汲", "老邴", "老糜", "老松",
        "大井", "大段", "大富", "大巫", "大乌", "大焦", "大巴", "大弓",
        "小井", "小段", "小富", "大巫", "小乌", "小焦", "小巴", "小弓",
        "老井", "老段", "老富", "老巫", "老乌", "老焦", "老巴", "老弓",
        "大牧", "大隗", "大山", "大谷", "大车", "大侯", "大宓", "大蓬",
        "小牧", "小隗", "小山", "小谷", "小车", "小侯", "小宓", "小蓬",
        "老牧", "老隗", "老山", "老谷", "老车", "老侯", "老宓", "老蓬",
        "大全", "大郗", "大班", "大仰", "大秋", "大仲", "大伊", "大宫",
        "小全", "小郗", "小班", "小仰", "小秋", "小仲", "小伊", "小宫",
        "老全", "老郗", "老班", "老仰", "老秋", "老仲", "老伊", "老宫",
        "大宁", "大仇", "大栾", "大暴", "大甘", "大斜", "大厉", "大戎",
        "小宁", "小仇", "小栾", "小暴", "小甘", "小斜", "小厉", "小戎",
        "老宁", "老仇", "老栾", "老暴", "老甘", "老斜", "老厉", "老戎",
        "大祖", "大武", "大符", "大刘", "大景", "大詹", "大束", "大龙",
        "小祖", "小武", "小符", "小刘", "小景", "小詹", "小束", "小龙",
        "老祖", "老武", "老符", "老刘", "老景", "老詹", "老束", "老龙",
        "大叶", "大幸", "大司", "大韶", "大郜", "大黎", "大蓟", "大薄",
        "小叶", "小幸", "小司", "小韶", "小郜", "小黎", "小蓟", "小薄",
        "老叶", "老幸", "老司", "老韶", "老郜", "老黎", "老蓟", "老薄",
        "大印", "大宿", "大白", "大怀", "大蒲", "大邰", "大从", "大鄂",
        "小印", "小宿", "小白", "小怀", "小蒲", "小邰", "小从", "小鄂",
        "老印", "老宿", "老白", "老怀", "老蒲", "老邰", "老从", "老鄂",
        "大索", "大咸", "大籍", "大赖", "大卓", "大蔺", "大屠", "大蒙",
        "小索", "小咸", "小籍", "小赖", "小卓", "小蔺", "小屠", "小蒙",
        "老索", "老咸", "老籍", "老赖", "老卓", "老蔺", "老屠", "老蒙",
        "大池", "大乔", "大阴", "大鬱", "大胥", "大能", "大苍", "大双",
        "小池", "小乔", "小阴", "小鬱", "小胥", "小能", "小苍", "小双",
        "老池", "老乔", "老阴", "老鬱", "老胥", "老能", "老苍", "老双",
        "大闻", "大莘", "大党", "大翟", "大谭", "大贡", "大劳", "大逄",
        "小闻", "小莘", "小党", "小翟", "小谭", "小贡", "小劳", "小逄",
        "老闻", "老莘", "老党", "老翟", "老谭", "老贡", "老劳", "老逄",
        "大姬", "大申", "大扶", "大堵", "大冉", "大宰", "大郦", "大雍",
        "小姬", "小申", "小扶", "小堵", "小冉", "小宰", "小郦", "小雍",
        "老姬", "老申", "老扶", "老堵", "老冉", "老宰", "老郦", "老雍",
        "大郤", "大璩", "大桑", "大桂", "大濮", "大牛", "大寿", "大通",
        "小郤", "小璩", "小桑", "小桂", "小濮", "小牛", "小寿", "小通",
        "老郤", "老璩", "老桑", "老桂", "老濮", "老牛", "老寿", "老通",
        "大边", "大扈", "大燕", "大冀", "大郏", "大浦", "大尚", "大农",
        "小边", "小扈", "小燕", "小冀", "小郏", "小浦", "小尚", "小农",
        "老边", "老扈", "老燕", "老冀", "老郏", "老浦", "老尚", "老农",
        "大温", "大别", "大庄", "大晏", "大柴", "大瞿", "大阎", "大充",
        "小温", "小别", "小庄", "小晏", "小柴", "小瞿", "小阎", "小充",
        "老温", "老别", "老庄", "老晏", "老柴", "老瞿", "老阎", "老充",
        "大慕", "大连", "大茹", "大习", "大宦", "大艾", "大鱼", "大容",
        "小慕", "小连", "小茹", "小习", "小宦", "小艾", "小鱼", "小容",
        "老慕", "老连", "老茹", "老习", "老宦", "老艾", "老鱼", "老容",
        "大向", "大古", "大易", "大慎", "大戈", "大廖", "大庾", "大终",
        "小向", "小古", "小易", "小慎", "小戈", "小廖", "小庾", "小终",
        "老向", "老古", "老易", "老慎", "老戈", "老廖", "老庾", "老终",
        "大暨", "大居", "大衡", "大步", "大都", "大耿", "大满", "大弘",
        "小暨", "小居", "小衡", "小步", "小都", "小耿", "小满", "小弘",
        "老暨", "老居", "老衡", "老步", "老都", "老耿", "老满", "老弘",
        "大匡", "大国", "大文", "大寇", "大广", "大禄", "大阙", "大东",
        "小匡", "小国", "小文", "小寇", "小广", "小禄", "小阙", "小东",
        "老匡", "老国", "老文", "老寇", "老广", "老禄", "老阙", "老东",
        "大欧", "大殳", "大沃", "大利", "大蔚", "大越", "大夔", "大隆",
        "小欧", "小殳", "小沃", "小利", "小蔚", "小越", "小夔", "小隆",
        "老欧", "老殳", "老沃", "老利", "老蔚", "老越", "老夔", "老隆",
        "大师", "大巩", "大厍", "大聂", "大晁", "大勾", "大敖", "大融",
        "小师", "小巩", "小厍", "小聂", "小晁", "小勾", "小敖", "小融",
        "老师", "老巩", "老厍", "老聂", "老晁", "老勾", "老敖", "老融",
        "大冷", "大訾", "大辛", "大阚", "大那", "大简", "大饶", "大空",
        "小冷", "小訾", "小辛", "小阚", "小那", "小简", "小饶", "小空",
        "老冷", "老訾", "老辛", "老阚", "老那", "老简", "老饶", "小空",
        "大曾", "大毋", "大沙", "大乜", "大养", "大鞠", "大须", "大丰",
        "小曾", "小毋", "小沙", "小乜", "小养", "小鞠", "小须", "小丰",
        "老曾", "老毋", "老沙", "老乜", "老养", "老鞠", "老须", "老丰",
        "大巢", "大关", "大蒯", "大相", "大查", "大后", "大荆", "大红",
        "小巢", "小关", "小蒯", "小相", "小查", "小后", "小荆", "小红",
        "老巢", "老关", "老蒯", "老相", "老查", "老后", "老荆", "老红",
        "大游", "大竺", "大权", "大逑", "大盖", "大益", "大桓", "大公",
        "小游", "小竺", "小权", "小逑", "小盖", "小益", "小桓", "小公",
        "老游", "老竺", "老权", "老逑", "老盖", "老益", "老桓", "老公",
        "大晋", "大楚", "大阎", "大法", "大汝", "大鄢", "大涂", "大钦",
        "小晋", "小楚", "小阎", "小法", "小汝", "小鄢", "小涂", "小钦",
        "老晋", "老楚", "老阎", "老法", "老汝", "老鄢", "老涂", "老钦",
        "大岳", "大帅", "大缑", "大亢", "大况", "大後", "大有", "大琴",
        "小岳", "小帅", "小缑", "小亢", "小况", "小後", "小有", "小琴",
        "老岳", "老帅", "老缑", "老亢", "老况", "老後", "老有", "老琴",
        "大商", "大牟", "大佘", "大佴", "大伯", "大赏", "大南", "大海",
        "小商", "小牟", "小佘", "小佴", "小伯", "小赏", "小南", "小海",
        "老商", "老牟", "老佘", "老佴", "老伯", "老赏", "老南", "老海",
        "大墨", "大哈", "大谯", "大笪", "大年", "大爱", "大阳", "大佟",
        "小墨", "小哈", "小谯", "小笪", "小年", "小爱", "小阳", "小佟",
        "老墨", "老哈", "老谯", "老笪", "老年", "老爱", "老阳", "老佟",
        };

        private string[] streetNames = { "人民", "解放", "中山", "和平", "中华", "新华", "民生", "建国", };

        private string[] streetDirs = { "路", "南路", "北路", "东路","西路", "中路", "大街", "街", "南街", "北街", "东街","西街", };

        public void Vote()
        {
            try
            {
                string name = "路牟";
                string phone = "13711291456";
                string auth = register(name, phone);
                string URLVote = "http://minisite.youku.com/yarislmvpk/api/vote.php";

                if (!string.IsNullOrEmpty(auth))
                {
                    using (WebClient webClient = new WebClient())
                    {
                        //            webClient.Headers["Host"] = "minisite.youku.com";
                        webClient.Headers["User-Agent"] = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0";
                        webClient.Headers["Accept"] = "application/json, text/javascript, */*; q=0.01";
                        webClient.Headers["Accept-Language"] = "en-US,en;q=0.5";
                        webClient.Headers["Accept-Encoding"] = "gzip, deflate";
                        //            webClient.Headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8";
                        webClient.Headers["X-Requested-With"] = "XMLHttpRequest";
                        webClient.Headers["Referer"] = "http://minisite.youku.com/yarislmvpk/";

                        NameValueCollection formData = new NameValueCollection();
                        formData["star_user"] = "bixia";
                        formData["uid"] = "6";
                        formData["vtype"] = "popularty";
                        formData["auth"] = auth + name + "__" + phone;

                        for (int i = 0; i < 2; i++)
                        {
                            byte[] responseBytes = webClient.UploadValues(URLVote, "POST", formData);
                            string result = Encoding.UTF8.GetString(responseBytes);

                            if (result.Contains("\"msg\":\"success\""))
                            {
                                Interlocked.Increment(ref successCount);
                            }
                            else
                            {
                                Interlocked.Increment(ref failureCount);
                                Console.Error.WriteLine("Cannot vote: " + result);
                                break;
                            }
                            if (i == 0)
                            {
                                Thread.Sleep(5010);
                            }
                        }
                    }

                }
                Interlocked.Increment(ref successIPCount);
            }
            catch (Exception e)
            {
                Interlocked.Increment(ref exceptionCount);                 
                    
                Console.Error.WriteLine(e);
            }
        }

        private string register(string userName, string phone)
        {
            string URLAuth = "http://minisite.youku.com/yarislmvpk/api/submit.php";
            string province = "内蒙古";
            string city = "呼和浩特";
            string address = "长城路213号";

            using (WebClient webClient = new WebClient())
            {
                //            webClient.Headers["Host"] = "minisite.youku.com";
                webClient.Headers["User-Agent"] = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0";
                webClient.Headers["Accept"] = "application/json, text/javascript, */*; q=0.01";
                webClient.Headers["Accept-Language"] = "en-US,en;q=0.5";
                webClient.Headers["Accept-Encoding"] = "gzip, deflate";
                //            webClient.Headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8";
                webClient.Headers["X-Requested-With"] = "XMLHttpRequest";
                webClient.Headers["Referer"] = "http://minisite.youku.com/yarislmvpk/";

                NameValueCollection formData = new NameValueCollection();
                formData["user_name"] = userName;
                formData["phone"] = phone;
                formData["province"] = province;
                formData["city"] = city;
                formData["address"] = address;

                byte[] responseBytes = webClient.UploadValues(URLAuth, "POST", formData);
                string resultAuthTicket = Encoding.UTF8.GetString(responseBytes);
                if (resultAuthTicket.Contains("\"msg\":\"success\""))
                {
                    string[] results = resultAuthTicket.Split(new char[] {'\\'});
                    if (results.Length > 0)
                    {
                        string[] auths = results[0].Split(new char[] { '\"' });
                        return auths[auths.Length - 1];
                    }
                    else
                    {
                        Console.Error.WriteLine("Cannot parse result: " + resultAuthTicket);
                        return null;
                    }
                }
                else
                {
                    Console.Error.WriteLine("Cannot register: " + resultAuthTicket);
                    return null;
                }
            }
        }

        internal static void CollectCounts(TimeSpan timespan)
        {
            totalSuccess += successCount;
            totalFailure += failureCount;
            totalException += exceptionCount;

            Console.WriteLine("Voted=" + totalSuccess + "/" + successCount + "IP: " + successIPCount +
                    " (" + timespan + ")" + " Failed=" + totalFailure + "/" + failureCount +
                    " Exception=" + totalException + "/" + exceptionCount);
            successIPCount = 0;
            successCount = 0;
            failureCount = 0;
            exceptionCount = 0;
        }

    }
}
