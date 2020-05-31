package com.zhuyun.rtp;

import java.util.Arrays;

public class SpsUtils {
	
	//视频帧率(FPS)获取
	/**
	 * 视频帧率信息在SPS的VUI parameters syntax中，需要根据time_scale、fixed_frame_rate_flag计算得到：
	 * fps = time_scale / num_units_in_tick。但是需要判断参数timing_info_present_flag是否存在，
	 * 若不存在表示FPS在信息流中无法获取。同时还存在另外一种情况：fixed_frame_rate_flag为1时，
	 * 两个连续图像的HDR输出时间频率为单位，获取的fps是实际的2倍。
	 */
	public static int getFPS(SpsBitStream bs, SpsInfoStruct info){
		int timing_info_present_flag = u(bs, 1);
		if (timing_info_present_flag != 0) {
			int num_units_in_tick = u(bs, 32);
			int time_scale = u(bs, 32);
			int fixed_frame_rate_flag = u(bs, 1);
			info.fps = (int)((float)time_scale / (float)num_units_in_tick);
			if (fixed_frame_rate_flag != 0) {
				info.fps = info.fps/2;
			}
		}
		return 0;
	}
 
	/**
	 移除H264的NAL防竞争码(0x03)
	 @param data sps数据
	 @param dataSize sps数据大小
	 */
	public static void delEmulationPrevention(byte[] data, int dataSize)
	{
	    int dataSizeTemp = dataSize;
	    for (int i=0, j=0; i<(dataSizeTemp-2); i++) {
	        int val = (data[i]^0x0) + (data[i+1]^0x0) + (data[i+2]^0x3);    //检测是否是竞争码
	        if (val == 0) {
	            for (j=i+2; j<dataSizeTemp-1; j++) {    //移除竞争码
	                data[j] = data[j+1];
	            }
	            
	            dataSize--;      //data size 减1
	        }
	    }
	}
 
	
	public static void spsBsInit(SpsBitStream bs, byte[] data, int size)
	{
	    if (bs != null) {
	        bs.data = data;
	        bs.size = size;
	        bs.index = 0;
	    }
	}
 
	/**
	 是否已经到数据流最后
	 @param bs sps_bit_stream数据
	 @return 1：yes，0：no
	 */
	public static boolean eof(SpsBitStream bs)
	{
	    return bs.index >= (bs.size * 8);    //位偏移已经超出数据
	}
 
	/**
	 读取从起始位开始的BitCount个位所表示的值
	 @param bs sps_bit_stream数据
	 @param bitCount bit位个数(从低到高)
	 @return value
	 */
	public static int u(SpsBitStream bs, int bitCount)
	{
	    int val = 0;
	    for (int i=0; i<bitCount; i++) {
	        val <<= 1;
	        if (eof(bs)) {
	            val = 0;
	            break;
	        } else if ((bs.data[bs.index / 8] & (0x80 >> (bs.index % 8))) != 0) {     //计算index所在的位是否为1
	            val |= 1;
	        }
	        bs.index++;  //递增当前起始位(表示该位已经被计算，在后面的计算过程中不需要再次去计算所在的起始位索引，缺点：后面每个bit位都需要去位移)
	    }
	    
	    return val;
	}
 
	/**
	 读取无符号哥伦布编码值(UE)
	 #2^LeadingZeroBits - 1 + (xxx)
	 @param bs sps_bit_stream数据
	 @return value
	 */
	public static int ue(SpsBitStream bs)
	{
	    int zeroNum = 0;
	    while (u(bs, 1) == 0 && !eof(bs) && zeroNum < 32) {
	        zeroNum ++;
	    }
	    
	    return (int)((1 << zeroNum) - 1 + u(bs, zeroNum));
	}
 
	/**
	 读取有符号哥伦布编码值(SE)
	 #(-1)^(k+1) * Ceil(k/2)
	 
	 @param bs sps_bit_stream数据
	 @return value
	 */
	public static int se(SpsBitStream bs)
	{
	    int ueVal = (int)ue(bs);
	    double k = ueVal;
	    
	    int seVal = (int)Math.ceil(k / 2);     //ceil:返回大于或者等于指定表达式的最小整数
	    if (ueVal % 2 == 0) {       //偶数取反，即(-1)^(k+1)
	        seVal = -seVal;
	    }
	    
	    return seVal;
	}
 
	/**
	 视频可用性信息(Video usability information)解析
	 @param bs sps_bit_stream数据
	 @param info sps解析之后的信息数据及结构体
	 @see E.1.1 VUI parameters syntax
	 */
	public static void vuiParaParse(SpsBitStream bs, SpsInfoStruct info)
	{
	    int aspect_ratio_info_present_flag = u(bs, 1);
	    if (aspect_ratio_info_present_flag != 0) {
	        int aspect_ratio_idc = u(bs, 8);
	        if (aspect_ratio_idc == 255) {      //Extended_SAR
	            u(bs, 16);      //sar_width
	            u(bs, 16);      //sar_height
	        }
	    }
	    
	    int overscan_info_present_flag = u(bs, 1);
	    if (overscan_info_present_flag != 0) {
	        u(bs, 1);       //overscan_appropriate_flag
	    }
	    
	    int video_signal_type_present_flag = u(bs, 1);
	    if (video_signal_type_present_flag != 0) {
	        u(bs, 3);       //video_format
	        u(bs, 1);       //video_full_range_flag
	        int colour_description_present_flag = u(bs, 1);
	        if (colour_description_present_flag != 0) {
	            u(bs, 8);       //colour_primaries
	            u(bs, 8);       //transfer_characteristics
	            u(bs, 8);       //matrix_coefficients
	        }
	    }
	    
	    int chroma_loc_info_present_flag = u(bs, 1);
	    if (chroma_loc_info_present_flag != 0) {
	        ue(bs);     //chroma_sample_loc_type_top_field
	        ue(bs);     //chroma_sample_loc_type_bottom_field
	    }
	 
	    int timing_info_present_flag = u(bs, 1);
	    if (timing_info_present_flag != 0) {
	        int num_units_in_tick = u(bs, 32);
	        int time_scale = u(bs, 32);
	        int fixed_frame_rate_flag = u(bs, 1);
	        
	        info.fps = (int)((float)time_scale / (float)num_units_in_tick);
	        if (fixed_frame_rate_flag != 0) {
	            info.fps = info.fps/2;
	        }
	    }
	    
	    int nal_hrd_parameters_present_flag = u(bs, 1);
	    if (nal_hrd_parameters_present_flag != 0) {
	        //hrd_parameters()  //see E.1.2 HRD parameters syntax
	    }
	    
	    //后面代码需要hrd_parameters()函数接口实现才有用
	    int vcl_hrd_parameters_present_flag = u(bs, 1);
	    if (vcl_hrd_parameters_present_flag != 0) {
	        //hrd_parameters()
	    }
	    if ((nal_hrd_parameters_present_flag !=0) || (vcl_hrd_parameters_present_flag != 0)) {
	        u(bs, 1);   //low_delay_hrd_flag
	    }
	    
	    u(bs, 1);       //pic_struct_present_flag
	    int bitstream_restriction_flag = u(bs, 1);
	    if (bitstream_restriction_flag != 0) {
	        u(bs, 1);   //motion_vectors_over_pic_boundaries_flag
	        ue(bs);     //max_bytes_per_pic_denom
	        ue(bs);     //max_bits_per_mb_denom
	        ue(bs);     //log2_max_mv_length_horizontal
	        ue(bs);     //log2_max_mv_length_vertical
	        ue(bs);     //max_num_reorder_frames
	        ue(bs);     //max_dec_frame_buffering
	    }
	}
 
	//See 7.3.1 NAL unit syntax
	//See 7.3.2.1.1 Sequence parameter set data syntax
	public static int h264ParseSps(byte[] data, int dataSize, SpsInfoStruct info)
	{
	    if ((data == null) || dataSize <= 0 || (info== null)) return 0;
	    int ret = 0;
	    
	    byte[] dataBuf = new byte[dataSize];
	    System.arraycopy(data, 0, dataBuf, 0, dataSize);	//重新拷贝一份数据，防止移除竞争码时对原数据造成影响
	    
	    delEmulationPrevention(dataBuf, dataSize);
	 
//	    sps_bit_stream bs = {0};
	    
	    SpsBitStream bs = new SpsBitStream();
	    spsBsInit(bs, dataBuf, dataSize);   //初始化SPS数据流结构体
	    
	    u(bs, 1);      //forbidden_zero_bit
	    u(bs, 2);      //nal_ref_idc
	    int nal_unit_type = u(bs, 5);
	 
	    if (nal_unit_type == 0x7) {     //Nal SPS Flag
	        info.profile_idc = u(bs, 8);
	        u(bs, 1);      //constraint_set0_flag
	        u(bs, 1);      //constraint_set1_flag
	        u(bs, 1);      //constraint_set2_flag
	        u(bs, 1);      //constraint_set3_flag
	        u(bs, 1);      //constraint_set4_flag
	        u(bs, 1);      //constraint_set4_flag
	        u(bs, 2);      //reserved_zero_2bits
	        info.level_idc = u(bs, 8);
	        
	        ue(bs);    //seq_parameter_set_id
	        
	        int chroma_format_idc = 1;     //摄像机出图大部分格式是4:2:0
	        if (info.profile_idc == 100 || info.profile_idc == 110 || info.profile_idc == 122 ||
	            info.profile_idc == 244 || info.profile_idc == 44 || info.profile_idc == 83 ||
	            info.profile_idc == 86 || info.profile_idc == 118 || info.profile_idc == 128 ||
	            info.profile_idc == 138 || info.profile_idc == 139 || info.profile_idc == 134 || info.profile_idc == 135) {
	            chroma_format_idc = ue(bs);
	            if (chroma_format_idc == 3) {
	                u(bs, 1);      //separate_colour_plane_flag
	            }
	            
	            ue(bs);        //bit_depth_luma_minus8
	            ue(bs);        //bit_depth_chroma_minus8
	            u(bs, 1);      //qpprime_y_zero_transform_bypass_flag
	            int seq_scaling_matrix_present_flag = u(bs, 1);
	            if (seq_scaling_matrix_present_flag != 0) {
	                int[] seq_scaling_list_present_flag = new int[8];
	                for (int i=0; i<((chroma_format_idc != 3)?8:12); i++) {
	                    seq_scaling_list_present_flag[i] = u(bs, 1);
	                    if (seq_scaling_list_present_flag[i] != 0) {
	                        if (i < 6) {    //scaling_list(ScalingList4x4[i], 16, UseDefaultScalingMatrix4x4Flag[i])
	                        } else {    //scaling_list(ScalingList8x8[i − 6], 64, UseDefaultScalingMatrix8x8Flag[i − 6] )
	                        }
	                    }
	                }
	            }
	        }
	        
	        ue(bs);        //log2_max_frame_num_minus4
	        int pic_order_cnt_type = ue(bs);
	        if (pic_order_cnt_type == 0) {
	            ue(bs);        //log2_max_pic_order_cnt_lsb_minus4
	        } else if (pic_order_cnt_type == 1) {
	            u(bs, 1);      //delta_pic_order_always_zero_flag
	            se(bs);        //offset_for_non_ref_pic
	            se(bs);        //offset_for_top_to_bottom_field
	            
	            int num_ref_frames_in_pic_order_cnt_cycle = ue(bs);
	            int[] offset_for_ref_frame = new int[num_ref_frames_in_pic_order_cnt_cycle * 4];
	            for (int i = 0; i<num_ref_frames_in_pic_order_cnt_cycle; i++) {
	                offset_for_ref_frame[i] = se(bs);
	            }
	        }
	        
	        ue(bs);      //max_num_ref_frames
	        u(bs, 1);      //gaps_in_frame_num_value_allowed_flag
	        
	        int pic_width_in_mbs_minus1 = ue(bs);     //第36位开始
	        int pic_height_in_map_units_minus1 = ue(bs);      //47
	        int frame_mbs_only_flag = u(bs, 1);
	        
	        info.width = (int)(pic_width_in_mbs_minus1 + 1) * 16;
	        info.height = (int)(2 - frame_mbs_only_flag) * (pic_height_in_map_units_minus1 + 1) * 16;
	        
	        if (frame_mbs_only_flag == 0) {
	            u(bs, 1);      //mb_adaptive_frame_field_flag
	        }
	        
	        u(bs, 1);     //direct_8x8_inference_flag
	        int frame_cropping_flag = u(bs, 1);
	        if (frame_cropping_flag != 0) {
	            int frame_crop_left_offset = ue(bs);
	            int frame_crop_right_offset = ue(bs);
	            int frame_crop_top_offset = ue(bs);
	            int frame_crop_bottom_offset= ue(bs);
	            
	            //See 6.2 Source, decoded, and output picture formats
	            int crop_unit_x = 1;
	            int crop_unit_y = 2 - frame_mbs_only_flag;      //monochrome or 4:4:4
	            if (chroma_format_idc == 1) {   //4:2:0
	                crop_unit_x = 2;
	                crop_unit_y = 2 * (2 - frame_mbs_only_flag);
	            } else if (chroma_format_idc == 2) {    //4:2:2
	                crop_unit_x = 2;
	                crop_unit_y = 2 - frame_mbs_only_flag;
	            }
	            
	            info.width -= crop_unit_x * (frame_crop_left_offset + frame_crop_right_offset);
	            info.height -= crop_unit_y * (frame_crop_top_offset + frame_crop_bottom_offset);
	        }
	        
	        int vui_parameters_present_flag = u(bs, 1);
	        if (vui_parameters_present_flag != 0) {
	            vuiParaParse(bs, info);
	        }
	     
	        ret = 1;
	    }
	    
	    return ret;
	}
	
	public static void main(String[] args) {
		SpsUtils spsUtils = new SpsUtils();
//		byte[] data = {0x27, 0x42, (byte)0x80, 0x2a, (byte) 0x8d, (byte) 0x95,
//						0x00, (byte) 0xf0, 0x04, 0x4f, (byte) 0xca, (byte) 0x80};
		
		byte[] data = {0x67,0x42,0x00,0x1f,(byte) 0x96,0x35,(byte) 0xc0,(byte) 0xa0,
					0x0b,0x74,(byte) 0xdc,0x04,0x04,0x04,0x08};
		int dataSize = data.length;
		SpsInfoStruct info = new SpsInfoStruct();
		int h264ParseSps = spsUtils.h264ParseSps(data, dataSize, info);
		System.out.println(info);
	}
	
}
