package com.example.ruju.myapplication;

        import android.content.Context;
        import android.graphics.Canvas;
        import android.graphics.Color;
        import android.graphics.Paint;
        import android.graphics.Paint.Align;
        import android.view.View;

/**
 * GraphView creates a scaled line or bar graph with x and y axis labels.
 * @author Arno den Hond
 *
 */
public class GraphView extends View {

    public static boolean BAR = false;
    public static boolean LINE = true;

    private Paint paint;
    private float[] valuesx;
    private float[] valuesy;
    private float[] valuesz;
    private String[] horlabels;
    private String[] verlabels;
    private String title;
    private boolean type;

    public GraphView(Context context, float[] xvalues, float[] yvalues,float[] zvalues, String title, String[] horlabels, String[] verlabels, boolean type) {
        super(context);
        if (xvalues == null)
            xvalues = new float[0];
        else
            this.valuesx = xvalues;
        if (yvalues == null)
            yvalues = new float[0];
        else
            this.valuesy = yvalues;
        if (zvalues == null)
            zvalues = new float[0];
        else
            this.valuesz = zvalues;
        if (title == null)
            title = "";
        else
            this.title = title;
        if (horlabels == null)
            this.horlabels = new String[0];
        else
            this.horlabels = horlabels;
        if (verlabels == null)
            this.verlabels = new String[0];
        else
            this.verlabels = verlabels;
        this.type = type;
        paint = new Paint();
    }

    public void setValues(float[] newxValues,float[] newyValues,float[] newzValues)
    {
        this.valuesx = newxValues;
        this.valuesy = newyValues;
        this.valuesz = newzValues;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float border = 50;
        float horstart = border * 2;
        float height = getHeight() - 1;
        float width = (getWidth() - 1);
        float max = getMax();
        float min = getMin();
        float diff = max - min;
        float graphheight = height - (2 * border);
        float graphwidth = width - (2 * border);
        paint.setColor(Color.BLACK);
        canvas.drawPaint(paint);
        paint.setTextAlign(Align.LEFT);
        int vers = verlabels.length - 1;
        for (int i = 0; i < verlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float y = ((graphheight / vers) * i) + border;
            canvas.drawLine(horstart, y, width, y, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(50);
            canvas.drawText(verlabels[i], 0, y, paint);
        }
        int hors = horlabels.length - 1;
        for (int i = 0; i < horlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float x = ((graphwidth / hors) * i) + horstart;
            canvas.drawLine(x, height - border, x, border, paint);
            paint.setTextAlign(Align.CENTER);
            if (i == horlabels.length - 1)
                paint.setTextAlign(Align.RIGHT);
            if (i == 0)
                paint.setTextAlign(Align.LEFT);
            paint.setTextSize(50);
            paint.setColor(Color.WHITE);
            canvas.drawText(horlabels[i], x, height - 4, paint);
        }

        paint.setTextAlign(Align.CENTER);
        canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

        if (max != min) {
            paint.setColor(Color.LTGRAY);
            if (type == BAR) {
                float datalength = valuesx.length;
                float colwidth = (width - (2 * border)) / datalength;
                for (int i = 0; i < valuesx.length; i++) {
                    float val = valuesx[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    canvas.drawRect((i * colwidth) + horstart, (border - h) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), height - (border - 1), paint);
                }
            } else {
                float datalength = valuesx.length;
                float colwidth = (width - (2 * border)) / datalength;
                float halfcol = colwidth / 2;
                float lasth = 0;
                for (int i = 0; i < valuesx.length; i++) {
                    float val = valuesx[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    if (i > 0)
                        paint.setColor(Color.GREEN);
                    paint.setStrokeWidth(2.0f);

                    canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);

                    lasth = h;
                }

                for (int i = 0; i < valuesy.length; i++) {
                    float val = valuesy[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    if (i > 0)
                        paint.setColor(Color.BLUE);
                    paint.setStrokeWidth(2.0f);

                    canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);

                    lasth = h;
                }

                for (int i = 0; i < valuesz.length; i++) {
                    float val = valuesz[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    if (i > 0)
                        paint.setColor(Color.RED);
                    paint.setStrokeWidth(2.0f);

                    canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);

                    lasth = h;
                }
                /*datalength = valuesy.length;
                colwidth = (width - (2 * border)) / datalength;
                halfcol = colwidth / 2;
                lasth = 0;
                for (int i = 0; i < valuesy.length; i++) {
                    float val = valuesy[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    if (i > 0)
                        paint.setColor(Color.GREEN);
                    paint.setStrokeWidth(2.0f);

                    canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);
                    lasth = h;*/
                }
            }
        }



    private float getMax() {
        float largestx = Integer.MIN_VALUE;
        float largesty = Integer.MIN_VALUE;
        float largestz = Integer.MIN_VALUE;
        float largest = 0;
        for (int i = 0; i < valuesx.length; i++)
            if (valuesx[i] > largestx)
                largestx = valuesx[i];
        for (int i = 0; i < valuesy.length; i++)
            if (valuesy[i] > largesty)
                largesty = valuesx[i];
        for (int i = 0; i < valuesy.length; i++)
            if (valuesz[i] > largestz)
                largestz = valuesz[i];
        largest = Math.max(Math.max(largestx,largesty),largestz);
        return largest;
    }

    private float getMin() {
        float smallestx = Integer.MAX_VALUE;
        float smallesty = Integer.MAX_VALUE;
        float smallestz = Integer.MAX_VALUE;
        float smallest = 0;
        for (int i = 0; i < valuesx.length; i++)
            if (valuesx[i] < smallestx)
                smallestx = valuesx[i];
        for (int i = 0; i < valuesy.length; i++)
            if (valuesy[i] < smallesty)
                smallesty = valuesx[i];
        for (int i = 0; i < valuesy.length; i++)
            if (valuesz[i] < smallestz)
                smallestz = valuesz[i];
        smallest = Math.max(Math.min(smallestx,smallesty),smallestz);
//largest = 3000;
        return smallest;

    }


}
