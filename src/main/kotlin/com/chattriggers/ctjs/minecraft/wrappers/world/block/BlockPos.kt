package com.chattriggers.ctjs.minecraft.wrappers.world.block

import com.chattriggers.ctjs.minecraft.wrappers.entity.Entity
import com.chattriggers.ctjs.utils.vec.Vec3i
import net.minecraft.util.math.BlockPos as MCBlockPos

class BlockPos(x: Int, y: Int, z: Int) : Vec3i(x, y, z) {
    constructor(x: Number, y: Number, z: Number) : this(x.toInt(), y.toInt(), z.toInt())

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
    fun up(n: Int = 1) = offset(BlockFace.Up, n)

    @JvmOverloads
    fun down(n: Int = 1) = offset(BlockFace.Down, n)

    @JvmOverloads
    fun north(n: Int = 1) = offset(BlockFace.North, n)

    @JvmOverloads
    fun south(n: Int = 1) = offset(BlockFace.South, n)

    @JvmOverloads
    fun east(n: Int = 1) = offset(BlockFace.East, n)

    @JvmOverloads
    fun west(n: Int = 1) = offset(BlockFace.West, n)

    @JvmOverloads
    fun offset(facing: BlockFace, n: Int = 1): BlockPos {
        return BlockPos(x + facing.getOffsetX() * n, y + facing.getOffsetY() * n, z + facing.getOffsetZ() * n)
    }

    // TODO(breaking): Renamed from toMCBlockPos
    fun toMC() = MCBlockPos(x, y, z)
}
