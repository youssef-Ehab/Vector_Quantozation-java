import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

public class Quant {
    public int ImageLength;
    public int ImageWidth;
    Vector<double[][]> averages = new Vector<>();
    HashMap<double[][], Vector<double[][]>> map = new HashMap<>();
    HashMap<Integer, double[][]> AveragesCodes = new HashMap<>();
    HashMap<double[][], Integer> RevAveragesCodes = new HashMap<>();

    public static void main(String[] args) throws IOException {

        Quant q = new Quant();
        System.out.println("enter the number of blocks");
        int num;
        int length;
        int width;
        Scanner scanner = new Scanner(System.in);
        num = scanner.nextInt();
        System.out.println("enter the length and the width of the blocks");
        length = scanner.nextInt();
        width = scanner.nextInt();
        double[][] temp = q.readImage();
        double[][] CodedImage = new double[length][width];
        q.ImageLength = temp.length;
        q.ImageWidth = temp[0].length;

        Vector<double[][]> container = q.Blocks(temp, length, width);
        Vector<Integer> code;
        Vector<double[][]> DecompPixels = new Vector<>();

        q.FirstAverage(length, width, container);
        for (int i = 0; i < Math.log(num); i++) {
            q.split();

            q.CalcAverage(length, width);

        }
        double[][] fin;
        for (int i = 0; i < 10; i++) {

            q.Stabilize(container);
        }
        code = q.codebook(container);
        CodedImage = q.DisToCode(code, length, width);
        DecompPixels = q.decompress(CodedImage);

        fin = q.Dis(DecompPixels, length, width);
        q.writeImage(fin);
        q.WriteToFile(length, width, code);

    }


    public void WriteToFile(int length, int width, Vector<Integer> code) throws IOException {
        File file = new File("Data.txt");
        FileWriter writer = new FileWriter("Data.txt", true);
        writer.write(ImageLength);
        writer.write(' ');
        writer.write(ImageWidth);
        writer.write('\n');
        writer.write(length);
        writer.write(' ');
        writer.write(width);
        writer.write('\n');
        for (int i = 0; i < averages.size(); i++) {
            writer.write(String.valueOf(averages.elementAt(i)));
            writer.write(' ');
        }
        writer.write('\n');
        for (int i = 0; i < averages.size(); i++) {
            writer.write(RevAveragesCodes.get(averages.elementAt(i)));
            writer.write(' ');
        }
        writer.write('\n');
        for (int i = 0; i < code.size(); i++) {
            writer.write(code.elementAt(i));
            writer.write(' ');
        }
        writer.close();
    }

    public double[][] readImage() throws IOException {
        File file = new File("she.jpg");
        BufferedImage img = null;
        img = ImageIO.read(file);
        int width = img.getWidth();
        int height = img.getHeight();
        ImageLength = width;
        ImageWidth = height;
        double[][] Data = new double[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pix = img.getRGB(i, j);
                int alpha = (pix >> 24) & 0xff;
                int red = (pix >> 16) & 0xff;
                int green = (pix >> 8) & 0xff;
                int blue = pix & 0xff;

                Data[i][j] = red;

                pix = (alpha << 24) | (red << 16) | (green << 8) | blue;
                img.setRGB(i, j, pix);


            }
        }
        return Data;
    }

    public void writeImage(double[][] temp) throws IOException {
        BufferedImage img = new BufferedImage(temp.length, temp[0].length, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < temp.length; i++) {
            for (int j = 0; j < temp[0].length; j++) {
                int value = 0xff000000 | ((int) temp[i][j] << 16) | ((int) temp[i][j] << 8) | ((int) temp[i][j]);
                img.setRGB(i, j, value);
            }
        }
        ImageIO.write(img, "jpg", new File("ans.jpg"));
        System.out.println("Done");
    }

    public void Stabilize(Vector<double[][]> container) {
        int MinIndex = 0;
        double[][] avg = new double[container.elementAt(0).length][container.elementAt(0)[0].length];
        HashMap<double[][], Vector<double[][]>> TempMap = new HashMap<>();
        for (int i = 0; i < container.size(); i++) {
            double[][] temp = container.elementAt(i);
            double minDistance = 0;
            for (int j = 0; j < averages.size(); j++) {
                avg = averages.elementAt(j);
                double distance = 0;
                for (int k = 0; k < avg.length; k++) {
                    for (int l = 0; l < avg[0].length; l++) {
                        distance += Math.abs((avg[k][l] - temp[k][l]));
                    }
                }
                if (j == 0) {
                    minDistance = distance;
                    MinIndex = j;
                } else {
                    if (distance < minDistance) {
                        minDistance = distance;
                        MinIndex = j;
                    }
                }

            }
            Vector<double[][]> vec = TempMap.get(averages.elementAt(MinIndex));
            if (vec == null) vec = new Vector<>();
            vec.add(temp);
            TempMap.put(averages.elementAt(MinIndex), vec);
        }
        map = TempMap;
        CalcAverage(avg.length, avg[0].length);
        for (int i = 0; i < averages.size(); i++) {
            AveragesCodes.put(i, averages.elementAt(i));
            RevAveragesCodes.put(averages.elementAt(i), i);
        }

    }

    public Vector<double[][]> Blocks(double[][] Data, int length, int width) {
        Vector<double[][]> container = new Vector<>();
        for (int i = 0; i < Data.length; i += length) {
            for (int j = 0; j < Data[0].length; j += width) {
                double[][] temp = new double[length][width];
                for (int x = 0; x < length; x++) {
                    for (int y = 0; y < width; y++) {
                        temp[x][y] = Data[i + x][j + x];
                    }
                }
                container.add(temp);
            }
        }
        return container;
    }

    public void FirstAverage(int length, int width, Vector<double[][]> container) {
        double[][] average = new double[length][width];

        for (int i = 0; i < container.size(); i++) {
            for (int j = 0; j < length; j++) {
                for (int k = 0; k < width; k++) {
                    average[j][k] += container.elementAt(i)[j][k];
                }
            }
        }
        for (int i = 0; i < average.length; i++) {
            for (int j = 0; j < average[0].length; j++) {
                average[i][j] = average[i][j] / container.size();
            }
        }
        for (int i = 0; i < average.length; i++) {
            for (int j = 0; j < average[0].length; j++) {
                System.out.println(average[i][j]);
            }
        }
        averages.add(average);
        map.put(average, container);
    }

    public void split() {
        Vector<double[][]> Temporary = new Vector<>();
        for (int i = 0; i < averages.size(); i++) {
            double[][] temp = averages.elementAt(i);

            double[][] temp1 = new double[temp.length][temp[0].length];
            double[][] temp2 = new double[temp.length][temp[0].length];

            for (int j = 0; j < temp1.length; j++) {
                for (int k = 0; k < temp1[0].length; k++) {
                    temp1[j][k] = temp[j][k] - 1;
                    temp2[j][k] = temp[j][k] + 1;
                }
            }
            Temporary.add(temp1);
            Temporary.add(temp2);
            Vector<double[][]> vec = map.get(temp);
            map.remove(temp);
            map.put(temp1, new Vector<>());
            map.put(temp2, new Vector<>());
            euclidean(vec, temp1, temp2);
        }
        averages = Temporary;
    }


    public void euclidean(Vector<double[][]> container, double[][] ceil, double[][] floor) {

        double dist1 = 0, dist2 = 0;

        if (container == null) {
            container = new Vector<>();
        }
        for (int l = 0; l < container.size(); l++) {
            double[][] block = container.elementAt(l);
            for (int j = 0; j < block.length; j++) {
                for (int k = 0; k < block[0].length; k++) {
                    dist1 += Math.abs((block[j][k] - ceil[j][k]));
                    dist2 += Math.abs((block[j][k] - floor[j][k]));
                }
            }
            if (dist1 < dist2) {
                Vector<double[][]> currentAvgBlocks = map.get(ceil);
                if (currentAvgBlocks == null) {
                    currentAvgBlocks = new Vector<>();
                }

                currentAvgBlocks.add(block);
                map.put(ceil, currentAvgBlocks);

            } else {
                Vector<double[][]> currentAvgBlocks = map.get(floor);
                if (currentAvgBlocks == null) {
                    currentAvgBlocks = new Vector<>();
                }

                currentAvgBlocks.add(block);
                map.put(floor, currentAvgBlocks);
            }
        }
    }


    public void CalcAverage(int length, int width) {
        HashMap<double[][], Vector<double[][]>> tempMap = new HashMap<>();
        for (int i = 0; i < averages.size(); i++) {
            Vector<double[][]> currentAvgBlocks = map.get(averages.get(i));
            if (currentAvgBlocks == null) continue;

            double[][] newAverage = new double[length][width];
            for (double[][] block : currentAvgBlocks) {
                for (int j = 0; j < length; j++) {
                    for (int k = 0; k < width; k++) {
                        newAverage[j][k] += block[j][k];
                    }
                }
            }

            for (int j = 0; j < length; j++) {
                for (int k = 0; k < width; k++) {
                    if (currentAvgBlocks.size() == 0) {
                        continue;
                    }
                    newAverage[j][k] /= currentAvgBlocks.size();
                }
            }

            averages.set(i, newAverage);

            tempMap.put(newAverage, currentAvgBlocks);
        }
        map = tempMap;
    }


    public double[][] Dis(Vector<double[][]> container, int width, int height) {
        double[][] img = new double[ImageLength][ImageWidth];
        for (int i = 0; i < ImageLength; ) {
            for (int j = 0; j < ImageWidth; ) {
                for (int k = 0; k < container.size(); k++) {
                    double[][] temp = container.elementAt(k);
                    for (int l = 0; l < width; l++) {
                        for (int m = 0; m < height; m++) {
                            img[i + l][j + m] = temp[l][m];
                        }
                    }
                    j += height;
                    if (j == ImageWidth) {
                        j = 0;
                        i += width;
                    }
                    if (i == ImageLength) break;
                }
                break;
            }
            break;
        }
        return img;
    }

    public double[][] DisToCode(Vector<Integer> vec, int length, int width) {
        int x = ImageLength / length;
        int y = ImageWidth / width;
        double[][] temp = new double[x][y];
        int counter = 0;
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                temp[i][j] = vec.elementAt(counter);
                counter++;
            }
        }
        return temp;
    }

    public Vector<double[][]> decompress(double[][] compressedImg) {
        int temp;
        Vector<double[][]> ans = new Vector<>();
        System.out.println("DECOMPRESS 5");
        for (int k = 0; k < AveragesCodes.size(); k++) {
            System.out.println(k);
            double[][] temp00 = AveragesCodes.get(k);
            for (int i = 0; i < AveragesCodes.get(k).length; i++) {
                for (int j = 0; j < AveragesCodes.get(k)[0].length; j++) {
                    System.out.print(temp00[i][j]);
                }
                System.out.println();
            }
        }
        for (int i = 0; i < compressedImg.length; i++) {
            for (int j = 0; j < compressedImg[0].length; j++) {

                temp = (int) compressedImg[i][j];
                ans.add(AveragesCodes.get(temp));
            }


        }

        return ans;
    }

    public Vector<Integer> codebook(Vector<double[][]> container) {
        Vector<double[][]> Final = new Vector<>();
        Vector<Integer> codes = new Vector<>();
        Vector<double[][]> Temporary = new Vector<>();
        int min = 0;
        double[][] temp;
        double[][] avg;

        for (int i = 0; i < container.size(); i++) {
            temp = container.elementAt(i);
            for (int j = 0; j < averages.size(); j++) {
                avg = averages.elementAt(j);
                Temporary = map.get(avg);
                if (Temporary == null) {
                    Temporary = new Vector<>();
                } else {
                    if (Temporary.contains(temp)) {
                        Final.add(avg);
                        codes.add(RevAveragesCodes.get(avg));
                        break;
                    } else {
                        continue;
                    }
                }
            }
        }
        System.out.println("done from codebook");
        return codes;
    }

}
