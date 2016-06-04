package de.belu.appstarter.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipDirectory
{

    public static void zipDirectory(String directoryPath, String zipFileNamePath) throws IOException
    {
        File directoryToZip = new File(directoryPath);
        File zipFile = new File(zipFileNamePath);
        File zipFilePath = zipFile.getParentFile();
        if (!zipFilePath.exists())
        {
            zipFilePath.mkdirs();
        }
        if (zipFile.exists())
        {
            zipFile.delete();
        }

        List<File> fileList = new ArrayList<File>();
        getAllFiles(directoryToZip, fileList);
        writeZipFile(directoryToZip, fileList, zipFile);
    }



    /**
     * Extract a zip file to the given destination folder.
     * @param file zip file to extract
     * @param destination destinatin folder
     */
    public static void unZipDirectory(File file, File destination) throws IOException
    {
        ZipInputStream in = null;
        OutputStream out = null;
        try
        {
            // Open the ZIP file
            in = new ZipInputStream(new FileInputStream(file));

            ZipEntry entry = null;
            while ((entry = in.getNextEntry()) != null)
            {
                String outFilename = entry.getName();

                // Open the output file
                if (entry.isDirectory())
                {
                    new File(destination, outFilename).mkdirs();
                }
                else
                {
                    File outputFile = new File(destination, outFilename);
                    File outputFileFolder = outputFile.getParentFile();
                    if(!outputFileFolder.exists())
                    {
                        outputFileFolder.mkdirs();
                    }
                    if(outputFile.exists())
                    {
                        outputFile.delete();
                    }
                    out = new FileOutputStream(outputFile);

                    // Transfer bytes from the ZIP file to the output file
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }

                    // Close the stream
                    out.close();
                }
            }
        }
        finally
        {
            // Close the stream
            if (in != null)
            {
                in.close();
            }
            if (out != null)
            {
                out.close();
            }
        }
    }

    private static void getAllFiles(File dir, List<File> fileList)
    {
        File[] files = dir.listFiles();
        for (File file : files)
        {
            fileList.add(file);
            if (file.isDirectory())
            {
                getAllFiles(file, fileList);
            }
        }
    }

    private static void writeZipFile(File directoryToZip, List<File> fileList, File zipFile)
    {

        try
        {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : fileList)
            {
                if (file.exists() && !file.isDirectory())
                { // we only zip files, not directories
                    addToZip(directoryToZip, file, zos);
                }
            }

            zos.close();
            fos.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException, IOException
    {

        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                file.getCanonicalPath().length());
        System.out.println("Writing '" + zipFilePath + "' to zip file");
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0)
        {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }
}
