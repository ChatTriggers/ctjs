package com.chattriggers.ctjs.api.world.block

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.vec.Vec3i
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.Direction
import java.util.function.Predicate

enum class BlockFace(
    private val oppositeIndex: Int,
    val axisDirection: AxisDirection,
    val axis: Axis,
    val directionVec: Vec3i,
    override val mcValue: Direction
) : StringIdentifiable, CTWrapper<Direction> {
    DOWN(1, AxisDirection.NEGATIVE, Axis.Y, Vec3i(0, -1, 0), Direction.DOWN),
    UP(0, AxisDirection.POSITIVE, Axis.Y, Vec3i(0, 1, 0), Direction.UP),
    NORTH(3, AxisDirection.NEGATIVE, Axis.Z, Vec3i(0, 0, -1), Direction.NORTH),
    SOUTH(2, AxisDirection.POSITIVE, Axis.Z, Vec3i(0, 0, 1), Direction.SOUTH),
    WEST(5, AxisDirection.NEGATIVE, Axis.X, Vec3i(-1, 0, 0), Direction.WEST),
    EAST(4, AxisDirection.POSITIVE, Axis.X, Vec3i(1, 0, 0), Direction.EAST);

    fun getOpposite() = entries[oppositeIndex]

    fun getOffsetX() = directionVec.x

    fun getOffsetY() = directionVec.y

    fun getOffsetZ() = directionVec.z

    fun rotateAround(axis: Axis): BlockFace {
        return when (axis) {
            Axis.X -> if (this != WEST && this != EAST) rotateX() else this
            Axis.Y -> if (this != UP && this != DOWN) rotateY() else this
            Axis.Z -> if (this != NORTH && this != SOUTH) rotateZ() else this
        }
    }

    fun rotateX() = when (this) {
        DOWN -> SOUTH
        UP -> NORTH
        NORTH -> DOWN
        SOUTH -> UP
        else -> throw IllegalStateException("Cannot rotate $this around x-axis")
    }

    fun rotateY() = when (this) {
        NORTH -> EAST
        SOUTH -> WEST
        WEST -> NORTH
        EAST -> SOUTH
        else -> throw IllegalStateException("Cannot rotate $this around y-axis")
    }

    fun rotateZ() = when (this) {
        DOWN -> WEST
        UP -> EAST
        WEST -> UP
        EAST -> DOWN
        else -> throw IllegalStateException("Cannot rotate $this around z-axis")
    }

    override fun asString() = name.lowercase()

    enum class Plane : Predicate<BlockFace>, Iterable<BlockFace> {
        HORIZONTAL,
        VERTICAL;

        override fun test(t: BlockFace) = t.axis.plane == this

        fun facings() = when (this) {
            HORIZONTAL -> arrayOf(NORTH, EAST, WEST, SOUTH)
            VERTICAL -> arrayOf(UP, DOWN)
        }

        override fun iterator() = facings().iterator()
    }

    enum class AxisDirection(
        val offset: Int,
        override val mcValue: Direction.AxisDirection,
    ) : CTWrapper<Direction.AxisDirection> {
        POSITIVE(1, Direction.AxisDirection.POSITIVE),
        NEGATIVE(-1, Direction.AxisDirection.NEGATIVE);

        companion object {
            @JvmStatic
            fun fromMC(axisDirection: Direction.AxisDirection) = when (axisDirection) {
                Direction.AxisDirection.POSITIVE -> POSITIVE
                Direction.AxisDirection.NEGATIVE -> NEGATIVE
            }
        }
    }

    enum class Axis(
        val plane: Plane,
        override val mcValue: Direction.Axis,
    ) : Predicate<BlockFace>, StringIdentifiable, CTWrapper<Direction.Axis> {
        X(Plane.HORIZONTAL, Direction.Axis.X),
        Y(Plane.VERTICAL, Direction.Axis.Y),
        Z(Plane.HORIZONTAL, Direction.Axis.Z);

        fun isHorizontal() = plane == Plane.HORIZONTAL

        fun isVertical() = plane == Plane.VERTICAL

        override fun test(t: BlockFace) = t.axis == this

        override fun asString() = name.lowercase()

        companion object {
            @JvmStatic
            fun fromMC(axis: Direction.Axis) = when (axis) {
                Direction.Axis.X -> X
                Direction.Axis.Y -> Y
                Direction.Axis.Z -> Z
            }
        }
    }

    companion object {
        @JvmStatic
        fun fromMC(facing: Direction) = when (facing) {
            Direction.DOWN -> DOWN
            Direction.UP -> UP
            Direction.NORTH -> NORTH
            Direction.SOUTH -> SOUTH
            Direction.WEST -> WEST
            Direction.EAST -> EAST
        }
    }
}
