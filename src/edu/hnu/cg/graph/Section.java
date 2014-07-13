package edu.hnu.cg.graph;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;


public class Section {

	private int id;
	private int superstep;

	private String sectionFilename;
	private String vertexDataFilename;
	private String edgeDataFilename;
	private String fetchIndexFilename;

	private RandomAccessFile sectionFile;
	private MappedByteBuffer vertexInformationBuffer;
	private FileChannel vertexInfoFileChannel;

	private RandomAccessFile vertexDataFile;
	private MappedByteBuffer vertexDataBuffer;
	private FileChannel vertexDataFileChannel;

	private RandomAccessFile edgeDataFile;
	private MappedByteBuffer edgeDataBuffer;
	private FileChannel edgeDataFileChannel;
	
	private RandomAccessFile fetchIndexFile;
	private MappedByteBuffer indexBuffer;
	private FileChannel indexChannel;

	private volatile boolean loaded = false;
	private volatile boolean unloaded = false;

	public Section(int _id,String graphFiename) throws IOException {
		id = _id;
		superstep = 0;
		sectionFilename = Filename.getSectionFilename(graphFiename, id);
		vertexDataFilename = Filename.getSectionVertexDataFilename(graphFiename, id);
		edgeDataFilename = Filename.getSectionEdgeDataFilename(graphFiename, id, superstep);
		fetchIndexFilename = Filename.getSectionFetchIndexFilename(graphFiename, id);
		
		sectionFile = new RandomAccessFile(sectionFilename, "r");
		vertexDataFile = new RandomAccessFile(vertexDataFilename,"rw");
		edgeDataFile = new RandomAccessFile(edgeDataFilename,"rw");
		fetchIndexFile = new RandomAccessFile(fetchIndexFilename,"r");
	
	}

	public void setSuperstep(int step) {
		superstep = step;
	}

	// if this method is called , null pointer needed to be checked
	public MappedByteBuffer getVertexInformationBuffer() {
		return vertexInformationBuffer;
	}

	public MappedByteBuffer getVertexDataBuffer() {
		return vertexDataBuffer;
	}

	public MappedByteBuffer getEdgeDataBuffer() {
		return edgeDataBuffer;
	}

	public void load() throws IOException {
		// 载入section信息文件
		if (sectionFile != null) {
			vertexInfoFileChannel = sectionFile.getChannel();
			vertexInformationBuffer = vertexInfoFileChannel.map( MapMode.READ_ONLY, 0, sectionFile.length());
		} else {
			return;
		}
		
		if(fetchIndexFile!=null){
			indexChannel = fetchIndexFile.getChannel();
			indexBuffer = indexChannel.map(MapMode.READ_ONLY, 0, fetchIndexFile.length());
		}else{
			return ;
		}

		// 载入顶点value数据文件
		if (vertexDataFile != null) {
			vertexDataFileChannel = vertexDataFile.getChannel();
			vertexDataBuffer = vertexDataFileChannel.map(MapMode.READ_WRITE, 0, vertexDataFile.length());
		} else {
			return;
		}

		// 载入边权重(消息数据)文件
		if (edgeDataFile != null) {
			edgeDataFileChannel = edgeDataFile.getChannel();
			edgeDataBuffer = edgeDataFileChannel.map(MapMode.READ_WRITE, 0, edgeDataFile.length());
		} else {
			return;
		}

		loaded = true;

	}

	public boolean isLoaded() {
		return loaded;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void clean(final MappedByteBuffer buffer) {
		AccessController.doPrivileged(new PrivilegedAction() {
			@Override
			public Object run() {
				try {
					Method getCleanerMethod = buffer.getClass().getMethod("cleaner", new Class[0]);
					getCleanerMethod.setAccessible(true);
					sun.misc.Cleaner cleaner = (sun.misc.Cleaner) getCleanerMethod.invoke(buffer, new Object[0]);
					cleaner.clean();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}

	public void unload() throws IOException {

		// 关闭之前需要将内容更新到磁盘
		vertexDataBuffer.force();
		edgeDataBuffer.force();

		
		sectionFile.close();
		vertexDataFile.close();
		edgeDataFile.close();
		fetchIndexFile.close();

		vertexInfoFileChannel.close();
		vertexDataFileChannel.close();
		edgeDataFileChannel.close();
		indexChannel.close();

		clean(vertexInformationBuffer);
		clean(vertexDataBuffer);
		clean(edgeDataBuffer);
		clean(indexBuffer);

		unloaded = true;
		loaded = false;

	}

	public boolean isUnloaded() {
		return unloaded;
	}

}
