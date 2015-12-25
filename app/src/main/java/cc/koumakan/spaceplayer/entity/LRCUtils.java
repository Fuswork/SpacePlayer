package cc.koumakan.spaceplayer.entity;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * Created by lhq on 2015/12/24.
 */
public class LRCUtils {

    private Vector<LRCElement> lrcList = null;//多行歌词

    public LRCUtils(String path){
        lrcList = new Vector<LRCElement>();
        readLRC(path);
    }

    private void readLRC(String path){
        try{
            System.out.println("开始读取歌词： "+path);
            FileInputStream fileInputStream = new FileInputStream(path);
            int lenth = fileInputStream.available();
            byte[] buffer = new byte[lenth];
            lenth = fileInputStream.read(buffer);
            String str = new String(buffer, "UTF-8");
            System.out.println("读取歌词成功： "+str);
            System.out.println("开始分析歌词");
            AnalyzeLRC(str);
            System.out.println("歌词分析完成");
            if(lrcList.size() > 0) {
                Collections.sort(lrcList, new Comparator<LRCElement>() {
                    @Override
                    public int compare(LRCElement arg1, LRCElement arg2) {
                        if(arg1.timePoint < arg2.timePoint){
                            return -1;
                        }
                        else if(arg1.timePoint > arg2.timePoint){
                            return 1;
                        }
                        else{
                            return 0;
                        }
                    }
                });
            }
        }catch (Exception e){
            System.out.println("读取歌词失败！");
        }
    }

    private void AnalyzeLRC(String LRCText){
        /** 分行 **/
        String[] strLines = LRCText.split("\n");
        /**对每一行进行处理**/
        for(String strLine : strLines){
            System.out.println("@正在处理: "+strLine);
            /** 循环处理标签 **/
            while(strLine.contains("]") && strLine.contains("[")){

                int lSign = strLine.indexOf('[');
                int rSign = strLine.indexOf(']');
                /** 标签内容 **/
                String tagStr = strLine.substring(lSign+1, rSign);
                System.out.println("标签内容： "+tagStr);
                int fsSign = tagStr.indexOf(':');
                int stSign = tagStr.indexOf('.');

                String fPart, sPart, tPart;//标签中的三部分内容

                /** 分离标签 **/
                if(fsSign < 0) {//如果没有‘:’说明标签错误，跳过
                    continue;
                }else{
                    fPart = tagStr.substring(0, fsSign);
                }
                if(stSign > fsSign){
                    sPart = tagStr.substring(fsSign+1, stSign);
                    tPart = tagStr.substring(stSign+1);
                }else{
                    sPart = tagStr.substring(fsSign+1);
                    tPart = "0";
                }
                System.out.println("标签分离结果： "+fPart+"  "+sPart+"  "+tPart);
                int fTime, sTime, tTime, time;

                try{
                    fTime = Integer.parseInt(fPart);
                    sTime = Integer.parseInt(sPart);
                    tTime = Integer.parseInt(tPart);
                }catch (NumberFormatException e){ //转换错误，跳过该标签
                    continue;
                }

                time = (fTime*60+sTime)*1000+tTime*10;
                System.out.println("标签转换结果： "+time);
                /** 生成一个LRCElement，其lrcStr暂定为null **/
                LRCElement lrcElement = new LRCElement(null, time);
                lrcList.add(lrcElement);

                strLine = strLine.substring(rSign+1);
            }
            /** 此时strLine剩下的是歌词内容 **/
            System.out.println("回填");
            for(int i=lrcList.size()-1; i>=0; i--){
                if(lrcList.elementAt(i).lrcStr != null) break;
                lrcList.elementAt(i).lrcStr = strLine;
            }
        }
    }

    public String getLRCLine(int time){
        int i;
        for(i=0; i<lrcList.size(); i++){
            if(lrcList.elementAt(i).timePoint > time){
                break;
            }
        }
        if(i == 0) return null;
        else return lrcList.elementAt(i-1).lrcStr;
    }

    public void outList(){
        if(lrcList.size() == 0) {
            System.out.println("暂无歌词");
        }
        else {
            for (LRCElement lrcElement : lrcList) {
                System.out.println(lrcElement.timePoint+" - "+lrcElement.lrcStr);
            }
        }
    }

    /**
     * 一行歌词
     */
    private class LRCElement{
        private String lrcStr;
        private int timePoint;

        public LRCElement(){
            lrcStr = "";
            timePoint = 0;
        }
        public LRCElement(String lrcStr, int timePoint){
            this.lrcStr = lrcStr;
            this.timePoint = timePoint;
        }
    }
}
