package com.chattriggers.ctjs.api.world.block

import com.chattriggers.ctjs.api.CTWrapper
import com.chattriggers.ctjs.api.entity.Entity
import com.chattriggers.ctjs.api.vec.Vec3i
import com.chattriggers.ctjs.MCBlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.floor
import kotlin.math.sqrt

class BlockPos(x: Int, y: Int, z: Int) : Vec3i(x, y, z), CTWrapper<MCBlockPos> {
    override val mcValue = MCBlockPos(x, y, z)

    constructor(x: Number, y: Number, z: Number) : this(
        floor(x.toDouble()).toInt(),
        floor(y.toDouble()).toInt(),
        floor(z.toDouble()).toInt()
    )

    constructor(pos: Vec3i) : this(pos.x, pos.y, pos.z)

    constructor(pos: MCBlockPos) : this(pos.x, pos.y, pos.z)

    constructor(source: Entity) : this(source.getPos())

    override fun translated(dx: Int, dy: Int, dz: Int) = BlockPos(super.translated(dx, dy, dz))

    override fun scaled(scale: Int) = BlockPos(super.scaled(scale))

    override fun scaled(xScale: Int, yScale: Int, zScale: Int) = BlockPos(super.scaled(xScale, yScale, zScale))

    override fun crossProduct(other: Vec3i) = BlockPos(super.crossProduct(other))

    override operator fun unaryMinus() = BlockPos(super.unaryMinus())

    override operator fun plus(other: Vec3i) = BlockPos(super.plus(other))

    override operator fun minus(other: Vec3i) = BlockPos(super.minus(other))

    @JvmOverloads
    fun up(n: Int = 1) = offset(BlockFace.UP, n)

    @JvmOverloads
    fun down(n: Int = 1) = offset(BlockFace.DOWN, n)

    @JvmOverloads
    fun north(n: Int = 1) = offset(BlockFace.NORTH, n)

    @JvmOverloads
    fun south(n: Int = 1) = offset(BlockFace.SOUTH, n)

    @JvmOverloads
    fun east(n: Int = 1) = offset(BlockFace.EAST, n)

    @JvmOverloads
    fun west(n: Int = 1) = offset(BlockFace.WEST, n)

    @JvmOverloads
    fun offset(facing: BlockFace, n: Int = 1): BlockPos {
        return BlockPos(x + facing.getOffsetX() * n, y + facing.getOffsetY() * n, z + facing.getOffsetZ() * n)
    }

    fun distanceTo(other: BlockPos): Double {
        val x = (mcValue.x - other.x).toDouble()
        val y = (mcValue.y - other.y).toDouble()
        val z = (mcValue.z - other.z).toDouble()
        return sqrt(x * x + y * y + z * z)
    }

    fun toVec3d() = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())
}
