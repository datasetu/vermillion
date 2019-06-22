partition_list = []
range_list = []

def parts(n, k):
    
    global partition_list
	
    base_size = n/k
    remainder = n - (base_size*k)
	
    if remainder>0:
	partition_list = [base_size + 1 for _ in range(remainder)]
	partition_list = partition_list + [base_size for _ in range(k-remainder)]
	
    else:
	partition_list=[base_size for _ in range(k)]
	
    return partition_list
	
def ranges(n, k):

    global partition_list, range_list

    if not partition_list:
	parts(n,k)

    index = 0
    
    for n in partition_list:
	range_list.append([index, index + (n-1)])
	index = index + n 
	
    return range_list
