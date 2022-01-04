package com.example.android.wifirttscan.utils;

import com.fengmap.android.map.geometry.FMGeoCoord;

import java.util.ArrayList;
import java.util.List;

public class LocationAlgorithm {

    /**
     * https://blog.csdn.net/weixin_39588084/article/details/111555481
     * https://blog.csdn.net/qq_49864684/article/details/115870377
     * @param x1,y1,d1,x2,y2,d2,x3,y3,d3 3个AP的坐标和测量距离
     * @return STA预测x，y坐标
     * 缺点：三个圆相交于一点，理想化过于严重，导致误差较大
     */
    public static double[] trilateration(double x1, double y1, double d1, double x2, double y2,double d2, double x3, double y3, double d3){
        double []d={0.0,0.0};
        double a11 = 2*(x1-x3);
        double a12 = 2*(y1-y3);
        double b1 = Math.pow(x1,2) - Math.pow(x3,2) +Math.pow(y1,2)-Math.pow(y3,2) +Math.pow(d3,2)-Math.pow(d1,2);
        double a21 = 2*(x2-x3);
        double a22 = 2*(y2-y3);
        double b2 = Math.pow(x2,2) - Math.pow(x3,2) +Math.pow(y2,2)-Math.pow(y3,2) +Math.pow(d3,2)-Math.pow(d2,2);
        d[0]=(b1*a22-a12*b2)/(a11*a22-a12*a21);
        d[1]=(a11*b2-b1*a21)/(a11*a22-a12*a21);
        return d;
    }

    public static double euclid_distance(double[] x, double[] y){
        // 求 两点之间的欧式距离
        if(x.length==2 && y.length==2){
            return Math.sqrt(
                    Math.pow((x[0]-y[0]),2) + Math.pow((x[1]-y[1]),2)
            );
        }
        else if(x.length==3 && y.length==3){
            return Math.sqrt(
                    Math.pow((x[0]-y[0]),2) + Math.pow((x[1]-y[1]),2) + Math.pow((x[2]-y[2]),2)
            );
        }
        return -1;
    };

    static List<double[]> neighbourhood_points(List<double[]> points, double[] x_centroid,double threshold_distance){
        List<double[]> eligible_X = new ArrayList<>();
        for(double[] x : points){
            double distance_between =  euclid_distance(x, x_centroid);
            if(distance_between!=-1 && distance_between<= threshold_distance){
                eligible_X.add(x);
            }
        }
        return eligible_X;
    }

    static double gaussian_kernel(double distance,int bandwidth){
        return (1.0 / (bandwidth * Math.sqrt(2*Math.PI))) * Math.exp(-0.5 * Math.pow((distance/bandwidth),2));
    }


    public static double[] mean_shift(List<double[]> points, double[] init_point){
        double []iter_position = init_point;
        int look_distance = 5;  // 设置近邻点搜索阈值，5m以内算临近点
        int kernel_bandwidth = 25;  // 设置权重函数（高斯）的一个阈值

        List<double[]> X = new ArrayList<>(points);
        int n_iterations = 10; //迭代次数

        for(int n=0; n<n_iterations; n++){
            List<double[]> neighbours = neighbourhood_points(X,iter_position,look_distance);
            if(neighbours.size()==0) return iter_position;
            double[] numerator = new double[neighbours.get(0).length];
            double denominator = 0.0;
            for (double[] neighbour : neighbours){
                double distance = euclid_distance(neighbour,iter_position);
                double weight = gaussian_kernel(distance,kernel_bandwidth);
                //numerator += (weight*neighbour);
                for(int i=0;i<neighbour.length;i++){
                    neighbour[i] = neighbour[i] * weight;
                    numerator[i] += neighbour[i];
                }
                denominator += weight;
            }
            if(denominator>0){
                for(int i=0;i<iter_position.length;i++){
                    iter_position[i] = numerator[i] / denominator;
                }
            }
        }
        return iter_position;
    }



    public static void main(String[] args){
        double[] xy = LocationAlgorithm.trilateration(12641371.971, 4138703.5211, 6, 12641381.9026, 4138706.4714, 6, 12641370.7839, 4138708.7705, 6);

        System.out.println(xy[0]+"::"+xy[1]);

    }
}

