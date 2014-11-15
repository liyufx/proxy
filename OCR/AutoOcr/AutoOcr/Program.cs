using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Text;
using System.Threading;

namespace AutoOcr
{
    class Program
    {
        static void Main(string[] args)
        {
            cleanup(); 
            Console.Out.WriteLine("(V6) Waiting for image files");

            List<string> processedImages = new List<string>();
            Encoding utf8WithBom = new UTF8Encoding(true);

            for (int i=0; i<1800; i++)
            {
                try {
                    string[] files = Directory.GetFiles(".", "imacro*.ocr.*");

                    foreach (string filepath in files)
                    {
                        FileInfo fileInfo = new FileInfo(filepath);
                        FileInfo tempOutFileInfo = null;

                        string fileName = fileInfo.Name;

                        if (!processedImages.Contains(fileName))
                        {
                            processedImages.Add(fileName);
                            string extension = fileInfo.Extension;

                            string outFile = fileName.Substring(0, fileName.Length - extension.Length - ".ocr".Length) + "_tmpocr";
                            string finalOutFile = fileName.Substring(0, fileName.Length - extension.Length - ".ocr".Length) + "ocr.txt";

                            // Prepare the process to run
                            ProcessStartInfo start = new ProcessStartInfo();
                            // Enter in the command line arguments, everything you would enter after the executable name itself
                            start.Arguments = fileName + " " + outFile;
                            // Enter the executable to run, including the complete path
                            start.FileName = ".\\tesseract.exe";
                            // Do you want to show a console window?
                            start.WindowStyle = ProcessWindowStyle.Hidden;
                            start.CreateNoWindow = true;


                            // Run the external process & wait for it to finish
                            using (Process proc = Process.Start(start))
                            {
                                proc.WaitForExit(5000);

                                // Retrieve the app's exit code
                                if (new FileInfo(outFile + ".txt").Exists)
                                {
                                    tempOutFileInfo = new FileInfo(outFile + ".txt");
                                    using (StreamReader sr = new StreamReader(outFile + ".txt"))
                                    {
                                        string line = sr.ReadLine();
                                        if (line == null) line = "";
                                        line = line.Trim().Replace(" ", "").ToUpper();
                                        using (TextWriter sw = new StreamWriter(finalOutFile, false, utf8WithBom))
                                        {
                                            if (line.Length != 4)
                                            {
                                                sw.WriteLine("ERROR");
                                                Console.Out.WriteLine("(V6) OCR error: " + fileName + " to " + finalOutFile);
                                            }
                                            else
                                            {
                                                sw.WriteLine(line);
                                                Console.Out.WriteLine("(V6) Successfully converted " + fileName + " to " + finalOutFile);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        try
                        {
                            fileInfo.Delete();
                            if (tempOutFileInfo != null)
                            {
                                tempOutFileInfo.Delete();
                            }
                        }
                        catch (Exception e)
                        {
                            // ignore
                        }
                    }
                    Thread.Sleep(1000);
                } 
                catch (Exception e) 
                {
                    Console.Error.WriteLine("Ignore error: " + e.ToString());
                }
                
            }
            var exeName = Assembly.GetExecutingAssembly().Location;
            System.Diagnostics.Process.Start(exeName);
        }

        static void cleanup()
        {
            Console.Out.WriteLine("(V6) Cleaning up imacro*ocr.txt files");
            DateTime now = DateTime.Now;
            int count = 0;
            try {
                string[] files = Directory.GetFiles(".", "imacro*ocr.txt");
                foreach (string filepath in files)
                {
                    FileInfo fileInfo = new FileInfo(filepath);
                    fileInfo.Delete();
                    count++;
                }
                files = Directory.GetFiles(".", "imacro*.ocr.jpg");
                foreach (string filepath in files)
                {
                    FileInfo fileInfo = new FileInfo(filepath);
                    fileInfo.Delete();
                    count++;
                }
            }
            catch (Exception e) 
            {
                Console.Error.WriteLine("Ignore error: " + e.ToString());
            }
            Console.Out.WriteLine("(V6) File deleted: " + count + " in " + (DateTime.Now - now));
        }
    }
}
