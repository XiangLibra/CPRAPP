/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package lyi.linyi.posemon

import android.graphics.*
import lyi.linyi.posemon.data.BodyPart
import lyi.linyi.posemon.data.Person
import kotlin.math.max

object VisualizationUtils {
    /** Radius of circle used to draw keypoints.  */
    private const val CIRCLE_RADIUS = 6f

    /** Width of line used to connected two keypoints.  */
    private const val LINE_WIDTH = 4f

    /** The text size of the person id that will be displayed when the tracker is available.  */
    private const val PERSON_ID_TEXT_SIZE = 30f

    /** Distance from person id to the nose keypoint.  */
    private const val PERSON_ID_MARGIN = 6f

    var pointA= PointF(3.5f, 2.0f)
    var pointB=PointF(3.5f, 2.0f)
    var leftpointAA=PointF(3.5f, 2.0f)
    var leftpointBB=PointF(3.5f, 2.0f)
    var leftpointCC=PointF(3.5f, 2.0f)
    var rightpointAA=PointF(3.5f, 2.0f)
    var rightpointBB=PointF(3.5f, 2.0f)
    var rightpointCC=PointF(3.5f, 2.0f)


    /** Pair of keypoints to draw lines between.  */
    private val bodyJoints = listOf(//控制連接的線
//        Pair(BodyPart.NOSE, BodyPart.LEFT_EYE),
//        Pair(BodyPart.NOSE, BodyPart.RIGHT_EYE),
//        Pair(BodyPart.LEFT_EYE, BodyPart.LEFT_EAR),
//        Pair(BodyPart.RIGHT_EYE, BodyPart.RIGHT_EAR),
//        Pair(BodyPart.NOSE, BodyPart.LEFT_SHOULDER),
//        Pair(BodyPart.NOSE, BodyPart.RIGHT_SHOULDER),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW),
        Pair(BodyPart.LEFT_ELBOW, BodyPart.LEFT_WRIST),
        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
        Pair(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST),
        Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
//        Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP),
//        Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_HIP),
//        Pair(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP),
//        Pair(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE),
//        Pair(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE),
//        Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE),
//        Pair(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE)
    )
    private val bodypoint = listOf(//控制關鍵點的顯示

        BodyPart.LEFT_SHOULDER,
        BodyPart.LEFT_ELBOW,
        BodyPart.LEFT_WRIST,

        BodyPart.RIGHT_ELBOW ,
        BodyPart.RIGHT_WRIST,
        BodyPart.RIGHT_SHOULDER,
//        BodyPart.LEFT_HIP,
//        BodyPart.RIGHT_HIP,
//        BodyPart.LEFT_KNEE,
//        BodyPart.LEFT_ANKLE

    )
    private val bodyLeft = Triple(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_ELBOW,BodyPart.LEFT_WRIST)
    private val bodyRight = Triple(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW,BodyPart.RIGHT_WRIST)

    // Draw line and point indicate body pose
    fun drawBodyKeypoints(
        input: Bitmap,
        persons: List<Person>,
        isTrackerEnabled: Boolean = false
    ): Bitmap {
        val paintCircle = Paint().apply {
            strokeWidth = CIRCLE_RADIUS
            color = Color.RED
            style = Paint.Style.FILL
        }
        val paintLine = Paint().apply {
            strokeWidth = LINE_WIDTH
            color = Color.RED
            style = Paint.Style.STROKE
        }

        val paintText = Paint().apply {
            textSize = PERSON_ID_TEXT_SIZE
            color = Color.BLUE
            textAlign = Paint.Align.LEFT
        }

        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val originalSizeCanvas = Canvas(output)
        persons.forEach { person ->
            // draw person id if tracker is enable
            if (isTrackerEnabled) {
                person.boundingBox?.let {
                    val personIdX = max(0f, it.left)
                    val personIdY = max(0f, it.top)

                    originalSizeCanvas.drawText(
                        person.id.toString(),
                        personIdX,
                        personIdY - PERSON_ID_MARGIN,
                        paintText
                    )
                    originalSizeCanvas.drawRect(it, paintLine)
                }
            }
            bodyJoints.forEach {
                pointA = person.keyPoints[it.first.position].coordinate
                pointB = person.keyPoints[it.second.position].coordinate
                val pointAB=Math.sqrt(Math.pow((pointA.x-pointB.x).toDouble(),2.0)+Math.pow((pointA.y-pointB.y).toDouble(),2.0))
                originalSizeCanvas.drawLine(pointA.x, pointA.y, pointB.x, pointB.y, paintLine)
            }
            bodypoint.forEach{
                originalSizeCanvas.drawCircle(
                    person.keyPoints[it.position].coordinate.x,
                    person.keyPoints[it.position].coordinate.y,
                    CIRCLE_RADIUS,
                    paintCircle)
            }

            leftpointAA= person.keyPoints[bodyLeft.first.position].coordinate
            leftpointBB= person.keyPoints[bodyLeft.second.position].coordinate
            leftpointCC= person.keyPoints[bodyLeft.third.position].coordinate
            rightpointAA= person.keyPoints[bodyRight.first.position].coordinate
            rightpointBB= person.keyPoints[bodyRight.second.position].coordinate
            rightpointCC= person.keyPoints[bodyRight.third.position].coordinate




//            person.keyPoints.forEach { point ->
//                originalSizeCanvas.drawCircle(
//                    point.coordinate.x,
//                    point.coordinate.y,
//                    CIRCLE_RADIUS,
//                    paintCircle
//                )
//            }
        }
        return output
    }
    fun bodys(
        input: Bitmap,
        persons: List<Person>,
        isTrackerEnabled: Boolean = false
    ):  Triple<PointF, PointF, PointF> {
        val paintCircle = Paint().apply {
            strokeWidth = CIRCLE_RADIUS
            color = Color.RED
            style = Paint.Style.FILL
        }
        val paintLine = Paint().apply {
            strokeWidth = LINE_WIDTH
            color = Color.RED
            style = Paint.Style.STROKE
        }

        val paintText = Paint().apply {
            textSize = PERSON_ID_TEXT_SIZE
            color = Color.BLUE
            textAlign = Paint.Align.LEFT
        }

        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val originalSizeCanvas = Canvas(output)
        persons.forEach { person ->
            // draw person id if tracker is enable
            if (isTrackerEnabled) {
                person.boundingBox?.let {
                    val personIdX = max(0f, it.left)
                    val personIdY = max(0f, it.top)

                    originalSizeCanvas.drawText(
                        person.id.toString(),
                        personIdX,
                        personIdY - PERSON_ID_MARGIN,
                        paintText
                    )
                    originalSizeCanvas.drawRect(it, paintLine)
                }
            }
            bodyJoints.forEach {
                pointA = person.keyPoints[it.first.position].coordinate
                pointB = person.keyPoints[it.second.position].coordinate
                val pointAB=Math.sqrt(Math.pow((pointA.x-pointB.x).toDouble(),2.0)+Math.pow((pointA.y-pointB.y).toDouble(),2.0))
                originalSizeCanvas.drawLine(pointA.x, pointA.y, pointB.x, pointB.y, paintLine)
            }
            bodypoint.forEach{
                originalSizeCanvas.drawCircle(
                    person.keyPoints[it.position].coordinate.x,
                    person.keyPoints[it.position].coordinate.y,
                    CIRCLE_RADIUS,
                    paintCircle)
            }





//            person.keyPoints.forEach { point ->
//                originalSizeCanvas.drawCircle(
//                    point.coordinate.x,
//                    point.coordinate.y,
//                    CIRCLE_RADIUS,
//                    paintCircle
//                )
//            }
        }
        var pointout=Triple(leftpointAA,leftpointBB,leftpointCC)
        return pointout
    }
}
