//package com.expense.manager.util;
//
//import java.io.InputStream;
//
//public class ResponseUtil {
//
//
//    public static String convertChunkedData(InputStream inStream){
//        try {
//            String line;
//
//            do {
//                // read the chunk header
//                line = inStream.readLine();
//                if (line == null) {
//                    return false;
//                }
//                // ignore any extensions after the chunk size
//                int idx = line.indexOf(';');
//                if (idx != -1) {
//                    line = line.substring(0, idx);
//                }
//                // parse the chunk size
//                int chunkLength = Integer.parseInt(line, 16);
//                if (chunkLength < 0) {
//                    return false;
//                }
//                // has the last chunk been reached?
//                if (chunkLength == 0) {
//                    break;
//                }
//                // read the chunk data
//                byte[] chunk = new byte[chunkLength];
//                int offset = 0;
//                do {
//                    int bytesRead = inStream.read(chunk, offset, chunkLength-offset);
//                    if (bytesRead < 0) {
//                        return false;
//                    }
//                    offset += bytesRead;
//                } while (offset < chunkLength);
//                // burn a CRLF at the end of the chunk
//                inStream.readLine();
//                // now do something with the chunk...
//            } while (true);
//
//            // read trailing HTTP headers
//            do {
//                line = inStream.readLine();
//                if (line == null) {
//                    return false;
//                }
//                // has the last header been read?
//                if (line.isEmpty()) {
//                    break;
//                }
//                // process the line as needed...
//            } while (true);
//
//            // all done
//            return true;
//        }
//        catch (Exception e) {
//            return false;
//        }
//    }
//}
