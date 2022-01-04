package com.example.android.wifirttscan.utils;

import com.fengmap.android.map.geometry.FMMapCoord;
import com.fengmap.android.utils.FMLocateCoordTransformer;

/**
 * @author jyy
 * fengmap坐标转换
 */
public class FMCoordTransformer {
    /**原始坐标点**/
    private Double[] _oriOrigion;
    /**原始坐标系X轴向量**/
    private Double[] _oriAxisX;
    /**原始坐标系Y轴向量**/
    private Double[] _oriAxisY;
    /**原始坐标系XY轴长度(模)**/
    private Double[] _oriRange;

    /**目标坐标点**/
    private Double[] _tarOrigion;
    /**目标坐标系X轴向量**/
    private Double[] _tarAxisX;
    /**目标坐标系Y轴向量**/
    private Double[] _tarAxisY;
    /**目标坐标系XY轴长度(模)**/
    private Double[] _tarRange;


    /**
     * 获取向量长度
     * @param vector 向量
     * @return Double
     */
    private Double getVectorLen(Double[] vector){

        return Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1]);
    }

    /**
     * 坐标初始化
     * @param origonParas 原始坐标
     * @param targetParas 目标坐标
     */
    public  void init(Double[][] origonParas, Double[][] targetParas){

        if(origonParas.length!=3 || targetParas.length != 3) {
            return ;
        }

        _oriOrigion = origonParas[0];
        //原始坐标系X轴向量
        _oriAxisX = new Double[]{origonParas[1][0] - origonParas[0][0], origonParas[1][1] - origonParas[0][1]};
        //原始坐标系Y轴向量
        _oriAxisY = new Double[]{origonParas[2][0] - origonParas[0][0] ,origonParas[2][1] - origonParas[0][1]};
        //原始坐标系XY轴长度(模)
        _oriRange = new Double[]{getVectorLen(_oriAxisX),getVectorLen(_oriAxisY)};

        //目标坐标原点
        _tarOrigion = targetParas[0];
        //目标坐标系X轴向量
        _tarAxisX = new Double[]{targetParas[1][0] - targetParas[0][0] ,targetParas[1][1] - targetParas[0][1]};
        //目标坐标系X轴向量
        _tarAxisY = new Double[]{targetParas[2][0] - targetParas[0][0] ,targetParas[2][1] - targetParas[0][1]};
        //目标坐标系X轴向量
        _tarRange = new Double[]{getVectorLen(_tarAxisX),getVectorLen(_tarAxisY)};

        //向量单位化
        _oriAxisX[0] /= _oriRange[0]; _oriAxisX[1] /= _oriRange[0];
        _oriAxisY[0] /= _oriRange[1]; _oriAxisY[1] /= _oriRange[1];

        _tarAxisX[0] /= _tarRange[0]; _tarAxisX[1] /= _tarRange[0];
        _tarAxisY[0] /= _tarRange[1]; _tarAxisY[1] /= _tarRange[1];
    }

    /**
     * 坐标转换
     * @param origon 原始坐标
     * @return Double
     */
    public  Double[] transform(Double[] origon) {

        Double[] offset = {origon[0]-_oriOrigion[0],origon[1]-_oriOrigion[1]};
        Double offsetX = _oriAxisX[0]*offset[0] + _oriAxisX[1]*offset[1];
        Double offsetY = _oriAxisY[0]*offset[0] + _oriAxisY[1]*offset[1];
        Double[] offstRatio = {offsetX/_oriRange[0],offsetY/_oriRange[1]};

        Double[] tarOffset = {offstRatio[0]*_tarRange[0],offstRatio[1]*_tarRange[1]};
        Double[] tarCoord = {_tarOrigion[0]+_tarAxisX[0]*tarOffset[0]+_tarAxisY[0]*tarOffset[1],_tarOrigion[1]+_tarAxisX[1]*tarOffset[0]+_tarAxisY[1]*tarOffset[1]};

        return tarCoord;
    }

    /**
     *
     * @param longitude 经度
     * @param latitude 纬度
     * @return Fengmap坐标
     */
    public static FMMapCoord wgs2WebMercator(double longitude, double latitude){
        FMLocateCoordTransformer transformer = new FMLocateCoordTransformer();
        return transformer.wgs2WebMercator(longitude,latitude);
    }

    /**
     * 测试示例
     * @return void
     */
    public static void main(String[] args) throws Exception {


        FMCoordTransformer trasformer = new FMCoordTransformer();
        //原始坐标系参数 三个角点
        Double[][] origonParas =new Double[3][];
        origonParas[0]= new Double[]{0d, 0d};//坐标角点 原点
        origonParas[1]=new Double[]{100d,0d};//X轴终点原始坐标
        origonParas[2]=new Double[]{0d,200d};//Y轴终点原始坐标

        //目标坐标系参数 三个角点
        Double[][] targetParas =new Double[3][];
        targetParas[0]=new Double[]{13502836.4878,3658566.5691};//地图最小坐标角点
        targetParas[1]=new Double[]{13503636.768,3657259.108};//X轴终点地图坐标
        targetParas[2]=new Double[]{13501983.572,3658044.467};//Y轴终点地图坐标

        //转换器初始化
        trasformer.init(origonParas,targetParas);

        //原始坐标系中的坐标
        // double[] origon ={0,0}; //原点测试
        Double[] origon =new Double[]{0d,200d}; //Y轴终点测试
        // double[] origon ={100,0}; //X轴终点测试

        //转换后的目标坐标系坐标
        Double[] targetCoord = trasformer.transform(origon);

        System.out.println(targetCoord[0]+","+targetCoord[1]);
    }
}