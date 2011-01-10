/*
 * jgeom: Geometry Library fo Java
 * 
 * Copyright (C) 2005  Samuel Gerber
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package toxi.geom.nurbs;

import toxi.geom.Axis3D;
import toxi.geom.GMatrix;
import toxi.geom.GVector;
import toxi.geom.InterpolationException;
import toxi.geom.Origin3D;
import toxi.geom.ReadonlyVec3D;
import toxi.geom.SingularMatrixException;
import toxi.geom.Vec3D;
import toxi.geom.Vec4D;
import toxi.math.MathUtils;

/**
 * This class offers some static methods to create NurbsSurfaces and NurbsCurves
 * from diffrent inputs.
 * 
 * @author sg
 * @version 1.0
 */
public final class NurbsCreator {

    private static KnotVector averaging(final float uk[], final int p) {
        int m = uk.length + p;
        int n = uk.length - 1;
        float ip = 1f / p;
        float u[] = new float[m + 1];
        for (int i = 0; i <= p; i++) {
            u[u.length - 1 - i] = 1;
        }
        for (int j = 1; j <= n - p; j++) {
            float sum = 0;
            for (int i = j; i <= j + p - 1; i++) {
                sum += uk[i];
            }
            u[j + p] = sum * ip;
        }
        return new KnotVector(u, p);
    }

    private static float[] centripetal(Vec3D points[]) {
        int n = points.length - 1;
        float d = 0;
        float uk[] = new float[n + 1];
        uk[n] = 1;
        double tmp[] = new double[n];
        for (int k = 1; k <= n; k++) {
            tmp[k - 1] = Math.sqrt(points[k].distanceTo(points[k - 1]));
            d += tmp[k - 1];
        }
        d = 1f / d;
        for (int i = 1; i < n; i++) {
            uk[i] = uk[i - 1] + (float) (tmp[i - 1] * d);
        }
        return uk;
    }

    /**
     * Create an Arc.
     * 
     * @param o
     *            Origin to creat arc around
     * @param r
     *            Radius of the arc.
     * @param thetaStart
     *            Start angle of the arc in radians
     * @param thetaEnd
     *            End angle of the arc in radians. If end angle is smaller than
     *            start angle, the end angle is increased by 2*PI.
     * @return A NurbsCurve for the Arc.
     */
    public static NurbsCurve createArc(Origin3D o, float r, float thetaStart,
            float thetaEnd) {
        Vec3D tmp = new Vec3D();

        if (thetaEnd < thetaStart) {
            thetaEnd += MathUtils.TWO_PI;
        }
        double theta = thetaEnd - thetaStart;

        int narcs = 4;
        if (theta <= MathUtils.HALF_PI) {
            narcs = 1;
        } else if (theta <= MathUtils.PI) {
            narcs = 2;
        } else if (theta <= MathUtils.THREE_HALVES_PI) {
            narcs = 3;
        }
        double dtheta = theta / narcs;
        int n = 2 * narcs;
        double w1 = Math.cos(dtheta / 2);

        final float sinStart = (float) Math.sin(thetaStart);
        final float cosStart = (float) Math.cos(thetaStart);
        tmp.set(o.xAxis).scaleSelf(r * cosStart);
        Vec3D p0 = new Vec3D(o.origin).addSelf(tmp);
        tmp.set(o.yAxis).scaleSelf(r * sinStart);
        p0.addSelf(tmp);

        tmp.set(o.yAxis).scaleSelf(cosStart);
        Vec3D t0 = new Vec3D(o.xAxis).scaleSelf(-sinStart).addSelf(tmp);

        Vec4D cps[] = new Vec4D[n + 1];
        cps[0] = new Vec4D(p0, 1);
        int index = 0;
        double angle = thetaStart;

        Vec3D p1 = new Vec3D();
        Vec3D p2 = new Vec3D();
        Vec3D t2 = new Vec3D();
        for (int i = 1; i <= narcs; i++) {
            angle += dtheta;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            tmp.set(o.xAxis).scaleSelf((float) (r * cos));
            p2.set(o.origin).addSelf(tmp);
            tmp.set(o.yAxis).scaleSelf((float) (r * sin));
            p2.addSelf(tmp);

            cps[index + 2] = new Vec4D(p2, 1);

            t2.set(o.xAxis).scaleSelf((float) -sin);
            tmp.set(o.yAxis).scaleSelf((float) cos);
            t2.addSelf(tmp);

            lineIntersect3D(p0, t0, p2, t2, p1, p1);

            cps[index + 1] = new Vec4D(p1, (float) w1);
            index += 2;
            if (i < narcs) {
                p0.set(p2);
                t0.set(t2);
            }
        }
        int j = n + 1;
        float uKnot[] = new float[j + 3];
        for (int i = 0; i < 3; i++) {
            uKnot[i + j] = 1;
        }
        switch (narcs) {
            case 2:
                uKnot[3] = 0.5f;
                uKnot[4] = 0.5f;
                break;
            case 3:
                uKnot[3] = uKnot[4] = MathUtils.THIRD;
                uKnot[5] = uKnot[6] = 2 * MathUtils.THIRD;
                break;
            case 4:
                uKnot[3] = 0.25f;
                uKnot[4] = 0.25f;
                uKnot[5] = 0.5f;
                uKnot[6] = 0.5f;
                uKnot[7] = 0.75f;
                uKnot[8] = 0.75f;
                break;
        }

        return new BasicNurbsCurve(cps, uKnot, 2);
    }

    /**
     * Create a full-circle NurbsCurve around the given Origin with radius r.
     * The NurbsCurve has controlpolygon which has 7 controlpoints and the shape
     * of quadrat.
     * 
     * @param o
     *            Origin to create the full-circle around
     * @param r
     *            Radius of the full-circle
     * @return A NurbsCurve for a full-circle
     */
    public static NurbsCurve createFullCircleQuad7(Origin3D o, float r) {

        Vec4D cp[] = new Vec4D[7];
        cp[0] = new Vec4D(o.xAxis.scale(r), 1);
        cp[3] = cp[0].getInvertedXYZ();
        cp[6] = cp[0].copy();

        cp[1] = new Vec4D(o.yAxis.add(o.xAxis).scaleSelf(r), 0.5f);
        cp[4] = cp[1].getInvertedXYZ();

        cp[2] =
                new Vec4D(o.xAxis.getInverted().addSelf(o.yAxis).scaleSelf(r),
                        0.5f);
        cp[5] = cp[2].getInvertedXYZ();

        for (int i = 0; i < 7; i++) {
            cp[i].addXYZSelf(o.origin);
        }
        float uQ7[] = { 0, 0, 0, 0.25f, 0.5f, 0.5f, 0.75f, 1, 1, 1 };
        return new BasicNurbsCurve(cp, uQ7, 2);
    }

    /**
     * Create a full-circle NurbsCurve around the given Origin with radius r.
     * The NurbsCurve has controlpolygon which has 9 controlpoints and the shape
     * of quadrat.
     * 
     * @param o
     *            Origin to create the full-circle around
     * @param r
     *            Radius of the full-circle
     * @return A NurbsCurve for a full-circle
     */
    public static NurbsCurve createFullCircleQuad9(Origin3D o, float r) {
        float w = (float) Math.sqrt(2) / 2;

        Vec4D cp[] = new Vec4D[9];
        cp[0] = new Vec4D(o.xAxis.scale(r), 1);
        cp[4] = cp[0].getInvertedXYZ();
        cp[8] = cp[0].copy();

        cp[1] = new Vec4D(o.xAxis.add(o.yAxis).scaleSelf(r), w);
        cp[5] = cp[1].getInvertedXYZ();

        cp[2] = new Vec4D(o.yAxis.scale(r), 1);
        cp[6] = cp[2].getInvertedXYZ();

        cp[3] =
                new Vec4D(o.xAxis.getInverted().addSelf(o.yAxis).scaleSelf(r),
                        w);
        cp[7] = cp[3].getInvertedXYZ();

        for (int i = 0; i < 9; i++) {
            cp[i].addXYZSelf(o.origin);
        }
        float uQ9[] =
                { 0, 0, 0, 0.25f, 0.25f, 0.5f, 0.5f, 0.75f, 0.75f, 1, 1, 1 };
        return new BasicNurbsCurve(cp, uQ9, 2);
    }

    /**
     * Create a revolved NurbsSurface from the given NurbsCurve around the given
     * axis whith the angle theta. [TODO:call createRevolvedSurface(Axis3D a,
     * NurbsCurve curve, double thetaStart, double thetaEnd) as as it is tested]
     * 
     * @param a
     *            Axis to revolve around.
     * @param curve
     *            NurbsCurve to revolve
     * @param theta
     *            Angle to revolve
     * @return The revolved NurbsSurface
     */
    public static NurbsSurface createRevolvedSurface(Axis3D a,
            NurbsCurve curve, double theta) {
        int narcs = 4;
        if (theta <= MathUtils.HALF_PI) {
            narcs = 1;
        } else if (theta <= MathUtils.PI) {
            narcs = 2;
        } else if (theta <= MathUtils.THREE_HALVES_PI) {
            narcs = 3;
        }

        double dtheta = theta / narcs;
        int j = 3 + 2 * (narcs - 1);
        float uKnot[] = new float[j + 3];
        for (int i = 0; i < 3; i++) {
            uKnot[j + i] = 1;
        }
        switch (narcs) {
            case 2:
                uKnot[3] = 0.5f;
                uKnot[4] = 0.5f;
                break;
            case 3:
                uKnot[3] = uKnot[4] = MathUtils.THIRD;
                uKnot[5] = uKnot[6] = 2 * MathUtils.THIRD;
                break;
            case 4:
                uKnot[3] = 0.25f;
                uKnot[4] = 0.25f;
                uKnot[5] = 0.5f;
                uKnot[6] = 0.5f;
                uKnot[7] = 0.75f;
                uKnot[8] = 0.75f;
                break;
        }

        double angle = 0;
        double cos[] = new double[narcs + 1];
        double sin[] = new double[narcs + 1];
        for (int i = 0; i <= narcs; i++) {
            cos[i] = Math.cos(angle);
            sin[i] = Math.sin(angle);
            angle += dtheta;
        }

        Vec4D pj[] = curve.getControlPoints();
        double wm = Math.cos(dtheta / 2);
        Vec3D O = new Vec3D();
        Vec3D P2 = new Vec3D();
        Vec3D T2 = new Vec3D();
        Vec3D P0 = new Vec3D();
        Vec3D T0 = new Vec3D();
        Vec3D tmp = new Vec3D();
        Vec3D X = new Vec3D();
        Vec3D Y = new Vec3D();
        Vec4D pij[][] = new Vec4D[2 * narcs + 1][pj.length];
        for (j = 0; j < pj.length; j++) {
            pointToLine3D(a.origin, a.dir, pj[j].to3D(), O);
            X.set(pj[j].to3D().subSelf(O));
            double r = X.magnitude();
            if (r == 0) {
                X.set(O);
            }
            X.normalize();
            a.dir.crossInto(X, Y);
            pij[0][j] = new Vec4D(pj[j]);
            P0 = pj[j].to3D();
            T0.set(Y);
            int index = 0;
            for (int i = 1; i <= narcs; i++) {
                tmp.set(X).scaleSelf((float) (r * cos[i]));
                P2.set(O).addSelf(tmp);
                tmp.set(Y).scaleSelf((float) (r * sin[i]));
                P2.addSelf(tmp);

                pij[index + 2][j] = new Vec4D(P2, pj[j].w);

                tmp.set(Y).scaleSelf((float) cos[i]);
                T2.set(X).scaleSelf((float) -sin[i]).addSelf(tmp);

                lineIntersect3D(P0, T0, P2, T2, tmp, tmp);
                pij[index + 1][j] = new Vec4D(tmp, (float) (wm * pj[j].w));

                index += 2;
                if (i < narcs) {
                    P0.set(P2);
                    T0.set(T2);
                }

            }
        }
        ControlNet cnet = new ControlNet(pij);
        return new BasicNurbsSurface(cnet, uKnot, curve.getKnots(), 2,
                curve.getDegree());
    }

    /**
     * Create a revolved NurbsSurface from the given NurbsCurve around the given
     * axis whith the angle theta. [TODO: test]
     * 
     * @param a
     *            Axis to revolve around.
     * @param curve
     *            NurbsCurve to revolve
     * @param thetaStart
     *            Angle to start revolution
     * @param thetaEnd
     *            Angle to end revolution
     * @return The revolved NurbsSurface
     */
    public static NurbsSurface createRevolvedSurface(Axis3D a,
            NurbsCurve curve, double thetaStart, double thetaEnd) {
        int narcs = 4;
        if (thetaStart > thetaEnd) {
            double tmp = thetaEnd;
            thetaEnd = thetaStart;
            thetaStart = tmp;
        }
        double theta = thetaEnd - thetaStart;
        if (theta <= MathUtils.HALF_PI) {
            narcs = 1;
        } else if (theta <= MathUtils.PI) {
            narcs = 2;
        } else if (theta <= MathUtils.THREE_HALVES_PI) {
            narcs = 3;
        }

        double dtheta = theta / narcs;
        int j = 3 + 2 * (narcs - 1);
        float uKnot[] = new float[j + 3];
        for (int i = 0; i < 3; i++) {
            uKnot[i] = 0;
            uKnot[j + i] = 1;
        }
        switch (narcs) {
            case 2:
                uKnot[3] = 0.5f;
                uKnot[4] = 0.5f;
                break;
            case 3:
                uKnot[3] = uKnot[4] = MathUtils.THIRD;
                uKnot[5] = uKnot[6] = 2 * MathUtils.THIRD;
                break;
            case 4:
                uKnot[3] = 0.25f;
                uKnot[4] = 0.25f;
                uKnot[5] = 0.5f;
                uKnot[6] = 0.5f;
                uKnot[7] = 0.75f;
                uKnot[8] = 0.75f;
                break;
        }

        double angle = thetaStart;
        double cos[] = new double[narcs + 1];
        double sin[] = new double[narcs + 1];
        for (int i = 0; i <= narcs; i++) {
            cos[i] = Math.cos(angle);
            sin[i] = Math.sin(angle);
            angle += dtheta;
        }

        Vec4D pj[] = curve.getControlPoints();
        double wm = Math.cos(dtheta / 2);
        Vec3D O = new Vec3D();
        Vec3D P2 = new Vec3D();
        Vec3D T2 = new Vec3D();
        Vec3D P0 = new Vec3D();
        Vec3D T0 = new Vec3D();
        Vec3D tmp = new Vec3D();
        Vec3D X = new Vec3D();
        Vec3D Y = new Vec3D();
        Vec4D pij[][] = new Vec4D[2 * narcs + 1][pj.length];
        for (j = 0; j < pj.length; j++) {
            pointToLine3D(a.origin, a.dir, pj[j].to3D(), O);
            X.set(pj[j].to3D().subSelf(O));
            double r = X.magnitude();
            if (r == 0) {
                X.set(O);
            }
            X.normalize();
            a.dir.crossInto(X, Y);
            pij[0][j] = new Vec4D(pj[j]);
            P0 = pj[j].to3D();
            T0.set(Y);
            int index = 0;
            for (int i = 1; i <= narcs; i++) {
                tmp.set(X).scaleSelf((float) (r * cos[i]));
                P2.set(O).addSelf(tmp);
                tmp.set(Y).scaleSelf((float) (r * sin[i]));
                P2.addSelf(tmp);

                pij[index + 2][j] = new Vec4D(P2, pj[j].w);

                tmp.set(Y).scaleSelf((float) cos[i]);
                T2.set(X).scaleSelf((float) -sin[i]).addSelf(tmp);

                lineIntersect3D(P0, T0, P2, T2, tmp, tmp);
                pij[index + 1][j] = new Vec4D(tmp, (float) (wm * pj[j].w));

                index += 2;
                if (i < narcs) {
                    P0.set(P2);
                    T0.set(T2);
                }

            }
        }
        ControlNet cnet = new ControlNet(pij);
        return new BasicNurbsSurface(cnet, uKnot, curve.getKnots(), 2,
                curve.getDegree());
    }

    /**
     * Create a semi-circle NurbsCurve around the given Origin with radius r.
     * 
     * @param o
     *            Origin to create semi-circle around.
     * @param r
     *            Radius of the semi-circle
     * @return A NurbsCurve for a semi-circle
     */
    public static NurbsCurve createSemiCircle(Origin3D o, float r) {
        Vec4D cp[] = new Vec4D[4];
        cp[0] = new Vec4D(o.xAxis.scale(r), 1);
        cp[3] = cp[0].getInvertedXYZ();
        cp[0].addXYZSelf(o.origin);
        cp[3].addXYZSelf(o.origin);
        cp[1] =
                new Vec4D(o.xAxis.add(o.yAxis).scaleSelf(r).addSelf(o.origin),
                        0.5f);
        cp[2] =
                new Vec4D(o.xAxis.getInverted().addSelf(o.yAxis).scaleSelf(r)
                        .addSelf(o.origin), 0.5f);

        float u[] = { 0, 0, 0, 0.5f, 1, 1, 1 };
        return new BasicNurbsCurve(cp, u, 2);
    }

    public static NurbsSurface createSwungSurface(NurbsCurve traj,
            NurbsCurve proj) {
        Vec4D[] cpProj = proj.getControlPoints();
        Vec4D[] cpTraj = traj.getControlPoints();

        Vec4D[][] cps = new Vec4D[cpProj.length][cpTraj.length];
        for (int i = 0; i < cpProj.length; i++) {
            for (int j = 0; j < cpTraj.length; j++) {
                Vec4D cp = new Vec4D();
                cp.x = cpProj[i].x * cpTraj[j].x;
                cp.y = cpProj[i].y * cpTraj[j].y;
                cp.z = cpProj[i].z * cpTraj[j].z;
                cp.w = cpProj[i].w * cpTraj[j].w;
                cps[i][j] = cp;
            }
        }
        return new BasicNurbsSurface(new ControlNet(cps), proj.getKnots(),
                traj.getKnots(), proj.getDegree(), traj.getDegree());

    }

    /**
     * Interpolates a NurbCurve form the given Points using a global
     * interpolation technique.
     * 
     * @param points
     *            Points to interpolate
     * @param p
     *            degree of the interpolated NurbsCurve
     * @return A NurbsCurve interpolating the given Points
     * @throws InterpolationException
     *             thrown if interpolation failed or is not possible.
     */
    public static NurbsCurve globalCurveInterpolation(Vec3D points[], int p)
            throws InterpolationException {
        try {

            int n = points.length - 1;
            double A[] = new double[(n + 1) * (n + 1)];

            float uk[] = centripetal(points);
            KnotVector uKnots = averaging(uk, p);
            for (int i = 0; i <= n; i++) {
                int span = uKnots.findSpan(uk[i]);
                double tmp[] = uKnots.basisFunctions(span, uk[i]);
                System.arraycopy(tmp, 0, A, i * (n + 1) + span - p, tmp.length);
            }
            GMatrix a = new GMatrix(n + 1, n + 1, A);
            GVector perm = new GVector(n + 1);
            GMatrix lu = new GMatrix(n + 1, n + 1);
            a.LUD(lu, perm);

            Vec4D cps[] = new Vec4D[n + 1];
            for (int i = 0; i < cps.length; i++) {
                cps[i] = new Vec4D(0f, 0f, 0f, 1f);
            }

            // x-ccordinate
            GVector b = new GVector(n + 1);
            for (int j = 0; j <= n; j++) {
                b.setElement(j, points[j].x);
            }
            GVector sol = new GVector(n + 1);
            sol.LUDBackSolve(lu, b, perm);
            for (int j = 0; j <= n; j++) {
                cps[j].x = (float) sol.getElement(j);
            }

            // y-ccordinate
            b = new GVector(n + 1);
            for (int j = 0; j <= n; j++) {
                b.setElement(j, points[j].y);
            }
            sol = new GVector(n + 1);
            sol.LUDBackSolve(lu, b, perm);
            for (int j = 0; j <= n; j++) {
                cps[j].y = (float) sol.getElement(j);
            }

            // z-ccordinate
            b = new GVector(n + 1);
            for (int j = 0; j <= n; j++) {
                b.setElement(j, points[j].z);
            }
            sol = new GVector(n + 1);
            sol.LUDBackSolve(lu, b, perm);
            for (int j = 0; j <= n; j++) {
                cps[j].z = (float) sol.getElement(j);
            }
            return new BasicNurbsCurve(cps, uKnots);
        } catch (SingularMatrixException ex) {
            throw new InterpolationException(ex);
        }

    }

    /**
     * Interpolates a NurbsSurface from the given points using a gloabl
     * interpolation technique.
     * 
     * @param points
     *            Points arranged in a net (matrix) to interpolate
     * @param p
     *            degree in u direction
     * @param q
     *            degree in v direction
     * @return A NurbsSurface interpolating the given points.
     * @throws InterpolationException
     *             thrown if interpolation failed or is not possible.
     */
    public static NurbsSurface globalSurfaceInterpolation(Vec3D points[][],
            int p, int q) throws InterpolationException {
        int n = points.length - 1;
        int m = points[0].length - 1;
        float[][] uv = surfaceMeshParameters(points, n, m);
        KnotVector u = averaging(uv[0], p);
        KnotVector v = averaging(uv[1], q);

        Vec4D[][] r = new Vec4D[m + 1][n + 1];
        Vec3D[] tmp = new Vec3D[n + 1];
        for (int l = 0; l <= m; l++) {
            for (int i = 0; i <= n; i++) {
                tmp[i] = points[i][l];
            }
            try {
                NurbsCurve curve = globalCurveInterpolation(tmp, p);
                r[l] = curve.getControlPoints();
            } catch (InterpolationException ex) {
                for (int i = 0; i < tmp.length; i++) {
                    r[l][i] = new Vec4D(tmp[i], 1);
                }
            }

        }

        Vec4D[][] cp = new Vec4D[n + 1][m + 1];
        tmp = new Vec3D[m + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) {
                tmp[j] = r[j][i].to3D();
            }
            try {
                NurbsCurve curve = globalCurveInterpolation(tmp, q);
                cp[i] = curve.getControlPoints();
            } catch (InterpolationException ex) {
                for (int j = 0; j < tmp.length; j++) {
                    cp[i][j] = new Vec4D(tmp[j], 1);
                }
            }
        }

        return new BasicNurbsSurface(new ControlNet(cp), u, v);
    }

    private static void lineIntersect3D(Vec3D p0, Vec3D t0, Vec3D p2, Vec3D t2,
            Vec3D out0, Vec3D out2) {
        Vec3D v02 = p0.sub(p2);

        double a = t0.dot(t0);
        double b = t0.dot(t2);
        double c = t2.dot(t2);
        double d = t0.dot(v02);
        double e = t2.dot(v02);
        double denom = a * c - b * b;

        double mu0, mu2;

        if (denom < MathUtils.EPS) {
            mu0 = 0;
            mu2 = b > c ? d / b : e / c;
        } else {
            mu0 = (b * e - c * d) / denom;
            mu2 = (a * e - b * d) / denom;
        }

        out0.set(t0.scale((float) mu0).addSelf(p0));
        out2.set(t2.scale((float) mu2).addSelf(p2));
    }

    private static void pointToLine3D(ReadonlyVec3D p, ReadonlyVec3D t,
            Vec3D top, Vec3D out) {
        Vec3D dir = top.sub(p);
        float hyp = dir.magnitude();
        out.set(p.add(t.scale(t.dot(dir.normalize()) * hyp)));
    }

    private static float[][] surfaceMeshParameters(Vec3D points[][], int n,
            int m) {
        float res[][] = new float[2][];
        int num = m + 1;
        float cds[] = new float[(n + 1) * (m + 1)];

        float uk[] = new float[n + 1];
        uk[n] = 1;
        for (int l = 0; l <= m; l++) {
            float total = 0;
            for (int k = 1; k <= n; k++) {
                cds[k] = points[k][l].distanceTo(points[k - 1][l]);
                total += cds[k];
            }
            if (total == 0) {
                num = num - 1;
            } else {
                float d = 0;
                total = 1f / total;
                for (int k = 1; k <= n; k++) {
                    d += cds[k];
                    uk[k] += d * total;
                }
            }
        }
        if (num == 0) {
            return null;
        }
        float inum = 1f / num;
        for (int k = 1; k < n; k++) {
            uk[k] *= inum;
        }

        num = n + 1;
        float vk[] = new float[m + 1];
        vk[m] = 1;
        for (int l = 0; l <= n; l++) {
            float total = 0;
            Vec3D[] pl = points[l];
            for (int k = 1; k <= m; k++) {
                cds[k] = pl[k].distanceTo(pl[k - 1]);
                total += cds[k];
            }
            if (total == 0) {
                num = num - 1;
            } else {
                float d = 0;
                total = 1f / total;
                for (int k = 1; k <= m; k++) {
                    d += cds[k];
                    vk[k] += d * total;
                }
            }
        }
        if (num == 0) {
            return null;
        }
        inum = 1f / num;
        for (int k = 1; k < m; k++) {
            vk[k] *= inum;
        }
        res[0] = uk;
        res[1] = vk;
        return res;
    }

    private NurbsCreator() {
    }

}
