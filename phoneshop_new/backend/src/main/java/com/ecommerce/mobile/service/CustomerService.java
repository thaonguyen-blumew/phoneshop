package com.ecommerce.mobile.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ecommerce.mobile.entity.Address;
import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.entity.Role;
import com.ecommerce.mobile.repository.AddressRepository;
import com.ecommerce.mobile.repository.CustomerRepository;
import com.ecommerce.mobile.repository.RoleRepository;
import org.springframework.transaction.annotation.Transactional;


// Luồng chính luôn là:
// Nhận request (có param or not) (từ controller)
/// xuống repository lấy dữ liệu (lúc này đã phải injection các repo vào service rồi.)
/// sau đó sử dụng dữ liệu đó tính toán, hay làm gì ko bt
/// rồi lưu vào db (save (dữ liệu entity hay lưu thẳng xuống DB gì đó như nhau hết))
/// rồi trả response cho bên trên (controller)
/// Có annotation Transactional thể hiện sự rollback.
@Service
public class CustomerService {
    @Autowired 
    private CustomerRepository customerRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired 
    private AddressRepository addressRepository;

    // Luồng đăng ký kiểm tra email -> lấy role -> tạo customer -> lưu DB
    @Transactional
    public Customer register(String email, String password, String fullName, String phone){
        if (password == null || password.length() < 6) {
            throw new RuntimeException("Mật khẩu phải từ 6 ký tự trở lên");
        }
        if (phone == null || !phone.matches("^(0|\\+84)[0-9]{9,10}$")) {
            throw new RuntimeException("Số điện thoại không hợp lệ");
        }
        // BƯỚC 1: duyệt email tài khoản
        if (customerRepository.existsByEmail(email)){
            throw new RuntimeException("Email này đã được đăng ký");
        }

        // CHỌN ROLE CUSTOMER CHO NGƯỜI NÀY. 
        Role role = roleRepository.findByNameRole("CUSTOMER").orElseThrow(() -> new RuntimeException("Lỗi, không tìm thấy role CUSTOMER"));

        // Bước 2: set các thứ MÀ CUSTOMER CẦN
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setHashPassword(passwordEncoder.encode(password));
        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setRole(role);
        customer.setIsActive(true);  

         // trả về tài khoản customer đã dă ký và lưu vào DB
    return customerRepository.save(customer);

    }

    @Transactional(readOnly = true)
    public Customer requireCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản khách hàng"));
    }

   // cập nhật thông tin
    @Transactional
    public Customer updateCustomerInfo(Long userId, String fullName, String phone){
        if (fullName == null || fullName.isBlank()) {
            throw new RuntimeException("Họ và tên không được để trống");
        }
        // gọi Customer từ DB (repository)
        @SuppressWarnings("null") // cảnh báo null cho userID
        Customer customer = customerRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan!"));
            customer.setFullName(fullName.trim());
            customer.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        
        return customerRepository.save(customer);

    }
        // ===== U11: DOI MAT KHAU =====
    // U11 ngoai le 6.1: phai kiem tra mat khau cu truoc khi doi
    @Transactional
    public Customer changePassword(Long userId, String oldPass, String newPass){
        Customer customer = customerRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            // Kiểm tra MK cũ
            if (!passwordEncoder.matches(oldPass, customer.getHashPassword())) {
                throw new RuntimeException("Mat khau hien tai khong chinh xac!");
            }
            // cập nhật mk mới
            customer.setHashPassword(passwordEncoder.encode(newPass));
            return customerRepository.save(customer);
        }

     // Thêm địa chỉ
    @Transactional
    public Address addAddress(String street, String city, Long customerId, String phone){
        return addAddress(customerId, street, null, null, city, phone, false);
    }

    @Transactional(readOnly = true)
    public List<Address> getAddresses(Long customerId) {
        return addressRepository.findByCustomerUserIDOrderByIsDefaultDescCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Address getAddressForCustomer(Long customerId, Long addressId) {
        return addressRepository.findByAddressIdAndCustomerUserID(addressId, customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));
    }

    @Transactional
    public Address addAddress(Long customerId,
                              String street,
                              String ward,
                              String district,
                              String city,
                              String phone,
                              boolean setDefault) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        Address address = new Address();
        address.setStreet(street == null ? null : street.trim());
        address.setWard(ward == null || ward.isBlank() ? null : ward.trim());
        address.setDistrict(district == null || district.isBlank() ? null : district.trim());
        address.setCity(city == null ? null : city.trim());
        address.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        address.setCustomer(customer);

        boolean hasAnyAddress = !addressRepository.findByCustomerUserID(customerId).isEmpty();
        address.setIsDefault(setDefault || !hasAnyAddress);

        if (address.getIsDefault()) {
            clearDefaultAddresses(customerId);
        }
        return addressRepository.save(address);
    }

    @Transactional
    public Address updateAddress(Long customerId,
                                 Long addressId,
                                 String street,
                                 String ward,
                                 String district,
                                 String city,
                                 String phone) {
        Address address = getAddressForCustomer(customerId, addressId);
        address.setStreet(street == null ? null : street.trim());
        address.setWard(ward == null || ward.isBlank() ? null : ward.trim());
        address.setDistrict(district == null || district.isBlank() ? null : district.trim());
        address.setCity(city == null ? null : city.trim());
        address.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        return addressRepository.save(address);
    }
            
        
        
        // Set địa chỉ mặc định   
    
    @Transactional
    public void setDefaultAddress(Long customerId, Long addressId){
        clearDefaultAddresses(customerId);
        Address address = getAddressForCustomer(customerId, addressId);
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long customerId, Long addressId) {
        Address address = getAddressForCustomer(customerId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByCustomerUserIDOrderByIsDefaultDescCreatedAtDesc(customerId);
            if (!remaining.isEmpty()) {
                Address first = remaining.get(0);
                first.setIsDefault(true);
                addressRepository.save(first);
            }
        }
    }

    private void clearDefaultAddresses(Long customerId) {
        List<Address> all = addressRepository.findByCustomerUserID(customerId);
        all.forEach(a -> a.setIsDefault(false));
        addressRepository.saveAll(all);
    }
}
